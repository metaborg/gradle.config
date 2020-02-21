package mb.gradle.config.devenv

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.*

class Repo(
  val name: String,
  val include: Boolean,
  val update: Boolean,
  val dirPath: String,
  val url: String,
  val branch: String
) {
  fun dir(rootDir: File) = rootDir.resolve(dirPath)

  fun isCheckedOut(rootDir: File) = dir(rootDir).exists()


  fun execGitCmd(rootProject: Project, vararg args: String, printCommandLine: Boolean = true) {
    execGitCmd(rootProject, mutableListOf(*args), printCommandLine)
  }

  fun execGitCmd(rootProject: Project, args: MutableList<String>, printCommandLine: Boolean = true) {
    rootProject.exec {
      this.executable = "git"
      this.workingDir = dir(rootProject.projectDir)
      this.args = args

      if(printCommandLine) {
        println(commandLine.joinToString(separator = " "))
      }
    }
  }


  fun execGitCmdInRoot(rootProject: Project, vararg args: String, printCommandLine: Boolean = true) {
    execGitCmdInRoot(rootProject, mutableListOf(*args), printCommandLine)
  }

  fun execGitCmdInRoot(rootProject: Project, args: MutableList<String>, printCommandLine: Boolean = true) {
    rootProject.exec {
      this.executable = "git"
      this.args = args

      if(printCommandLine) {
        println(commandLine.joinToString(separator = " "))
      }
    }
  }


  fun status(rootProject: Project) {
    execGitCmd(rootProject, "status", printCommandLine = false)
  }

  fun clone(rootProject: Project) {
    execGitCmdInRoot(rootProject, "clone", "--quiet", "--recurse-submodules", "--branch", branch, url, dirPath)
  }

  fun fetch(rootProject: Project) {
    execGitCmd(rootProject, "fetch", "--quiet", "--recurse-submodules")
  }

  fun checkout(rootProject: Project) {
    execGitCmd(rootProject, "checkout", "--quiet", branch)
  }

  fun pull(rootProject: Project) {
    execGitCmd(rootProject, "pull", "--quiet", "--recurse-submodules", "--rebase", "--autostash")
  }

  fun push(rootProject: Project) {
    execGitCmd(rootProject, "push")
  }

  fun pushAllTags(rootProject: Project) {
    execGitCmd(rootProject, "push", "--follow-tags")
  }

  fun pushAll(rootProject: Project) {
    execGitCmd(rootProject, "push", "--all")
  }


  override fun toString(): String {
    return String.format("  %1$-30s : include = %2$-5s, update = %3$-5s, branch = %4$-20s, path = %5$-30s, url = %6\$s", name, include, update, branch, dirPath, url)
  }
}

data class RepoConfig(
  var include: Boolean? = null,
  var update: Boolean? = null,
  var dirPath: String? = null,
  var url: String? = null,
  var branch: String? = null
)

fun repoProperties(rootDir: File): Properties {
  val propertiesFile = rootDir.resolve("repo.properties")
  if(!propertiesFile.isFile) {
    throw GradleException("Cannot create repositories of devenv; property file '$propertiesFile' does not exist or is not a file")
  }
  val properties = Properties()
  propertiesFile.inputStream().buffered().use { inputStream ->
    properties.load(inputStream)
  }
  return properties
}

fun toRepoConfigMap(properties: Properties): Map<String, RepoConfig> {
  val map = HashMap<String, RepoConfig>()
  for((k, v) in properties.entries) {
    k as String
    v as String
    when {
      k.endsWith(".update") -> {
        val name = k.substringBeforeLast('.')
        val config = map.getOrPut(name) { RepoConfig() }
        config.update = v == "true"
      }
      k.endsWith(".dir") -> {
        val name = k.substringBeforeLast('.')
        val config = map.getOrPut(name) { RepoConfig() }
        config.dirPath = v
      }
      k.endsWith(".url") -> {
        val name = k.substringBeforeLast('.')
        val config = map.getOrPut(name) { RepoConfig() }
        config.url = v
      }
      k.endsWith(".branch") -> {
        val name = k.substringBeforeLast('.')
        val config = map.getOrPut(name) { RepoConfig() }
        config.branch = v
      }
      else -> {
        val name = k
        val config = map.getOrPut(name) { RepoConfig() }
        config.include = v == "true"
      }
    }
  }
  return map
}

fun createRepos(repoConfigs: Map<String, RepoConfig>, repoProperties: Map<String, RepoConfig>, urlPrefix: String, rootBranch: String): List<Repo> {
  return repoConfigs.entries.map { (name, repoConfig) ->
    val (defaultInclude, defaultUpdate, defaultDirPath, defaultUrl, defaultBranch) = repoConfig
    val repoProperty = repoProperties[name]

    val include = repoProperty?.include ?: defaultInclude ?: false
    val update = repoProperty?.update ?: defaultUpdate ?: include
    val dirName = repoProperty?.dirPath ?: defaultDirPath ?: name
    val url = repoProperty?.url ?: defaultUrl ?: "$urlPrefix/$name.git"
    val branch = repoProperty?.branch ?: defaultBranch ?: rootBranch

    Repo(name, include, update, dirName, url, branch)
  }
}

fun branch(dir: File): String {
  try {
    FileRepositoryBuilder().readEnvironment().findGitDir(dir).setMustExist(true).build()
  } catch(e: RepositoryNotFoundException) {
    throw GradleException("Cannot retrieve current branch name because no git repository was found at '$dir'", e)
  }.use { repo ->
    // Use repository with 'use' to close repository after use, freeing up resources.
    val headRef = repo.exactRef(Constants.HEAD)
      ?: throw GradleException("Cannot retrieve current branch name because repository has no HEAD")
    if(headRef.isSymbolic) {
      return Repository.shortenRefName(headRef.target.name)
    } else {
      throw GradleException("Cannot retrieve current branch name because repository HEAD is not symbolic")
    }
  }
}
