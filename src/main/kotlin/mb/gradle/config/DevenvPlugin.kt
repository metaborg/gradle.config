package mb.gradle.config

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Suppress("unused")
class DevenvPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = DevenvExtension(project)
    project.extensions.add("devenv", extension)
    project.afterEvaluate {
      configure(this)
    }
  }


  open class GitTask : DefaultTask() {
    init {
      group = "devenv"
    }

    private val extension = project.extensions.getByType<DevenvExtension>()

    private val properties: Properties = run {
      val file = project.projectDir.resolve("repo.properties").toPath()
      properties(file)
    }


    val urlPrefix: String = extension.repoUrlPrefix
      ?: throw GradleException("Cannot update repositories of devenv; URL prefix has not been set")

    val rootBranch: String = try {
      gitBranch(project.projectDir)
    } catch(e: GradleException) {
      throw GradleException("Cannot update repositories of devenv; current branch cannot be retrieved", e)
    }

    val repos = extension.repos.map { toRepo(it, urlPrefix, rootBranch, properties) }


    private fun gitBranch(dir: File): String {
      return try {
        FileRepositoryBuilder().readEnvironment().findGitDir(dir).setMustExist(true).build()
      } catch(e: RepositoryNotFoundException) {
        throw GradleException("Cannot retrieve current branch name because no git repository was found at '$dir'", e)
      }.use { repo ->
        // Use repository with 'use' to close repository after use, freeing up resources.
        val headRef = repo.exactRef(Constants.HEAD)
          ?: throw GradleException("Cannot retrieve current branch name because repository has no HEAD")
        if(headRef.isSymbolic) {
          Repository.shortenRefName(headRef.target.name)
        } else {
          throw GradleException("Cannot retrieve current branch name because repository HEAD is not symbolic")
        }
      }
    }

    private fun properties(file: Path): Properties {
      val properties = Properties()
      if(!Files.isRegularFile(file)) {
        throw GradleException("Cannot update repositories of devenv; property file '$file' does not exist or is not a file")
      }
      Files.newInputStream(file).buffered().use { inputStream ->
        properties.load(inputStream)
      }
      return properties
    }

    private fun toRepo(repoConfig: RepoConfig, urlPrefix: String, rootBranch: String, properties: Properties): Repo {
      val (name, defaultUpdate, defaultUrl, defaultBranch, defaultDirPath) = repoConfig
      val update = "true" == properties.getProperty(name) ?: defaultUpdate ?: false
      val url = properties.getProperty("$name.url") ?: defaultUrl ?: "$urlPrefix/$name.git"
      val branch = properties.getProperty("$name.branch") ?: defaultBranch ?: rootBranch
      val dirName = properties.getProperty("$name.dir") ?: defaultDirPath ?: name
      return Repo(name, update, url, branch, dirName)
    }
  }

  private fun configure(project: Project) {
    project.tasks.register<GitTask>("repoList") {
      doLast {
        println("Git URL prefix: $urlPrefix")
        println("Current branch: $rootBranch")
        println("Repositories:")
        for(repo in repos) {
          println(repo)
        }
      }
      description = "Lists the Git repositories of devenv and their properties."
    }
    project.tasks.register<GitTask>("repoStatus") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Status for repository ${repo.dirPath}:")
          repo.status(project)
          println()
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: show its status"
    }
    project.tasks.register<GitTask>("repoClone") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          val dir = repo.dir(project)
          if(dir.exists()) continue
          println("Cloning repository ${repo.dirPath}:")
          repo.clone(project)
        }
      }
      description = "For each Git repository of devenv for which update is set to true: clone the repository if it has not been cloned yet."
    }
    project.tasks.register<GitTask>("repoUpdate") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          val dir = repo.dir(project)
          if(!dir.exists()) {
            println("Cloning repository ${repo.dirPath}:")
            repo.clone(project)
          } else {
            println("Updating repository ${repo.dirPath}:")
            repo.checkout(project)
            repo.pull(project)
          }
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: check out the repository to the correct branch and pull from origin, or clone the repository if it has not been cloned yet."
    }
  }
}


data class Repo(val name: String, val update: Boolean, val url: String, val branch: String, val dirPath: String) {
  fun dir(project: Project) = project.projectDir.resolve(dirPath)


  fun execGitCmd(project: Project, vararg args: String, printCommandLine: Boolean = true) {
    execGitCmd(project, mutableListOf(*args), printCommandLine)
  }

  fun execGitCmd(project: Project, args: MutableList<String>, printCommandLine: Boolean = true) {
    project.exec {
      this.executable = "git"
      this.workingDir = dir(project)
      this.args = args

      if(printCommandLine){
        println(commandLine.joinToString(separator = " "))
      }
    }
  }


  fun execGitCmdInRoot(project: Project, vararg args: String, printCommandLine: Boolean = true) {
    execGitCmdInRoot(project, mutableListOf(*args), printCommandLine)
  }

  fun execGitCmdInRoot(project: Project, args: MutableList<String>, printCommandLine: Boolean = true) {
    project.exec {
      this.executable = "git"
      this.args = args

      if(printCommandLine){
        println(commandLine.joinToString(separator = " "))
      }
    }
  }


  fun status(project: Project) {
    execGitCmd(project, "status", printCommandLine = false)
  }

  fun clone(project: Project) {
    execGitCmdInRoot(project, "clone", "--quiet", "--recurse-submodules", "--branch", branch, url, dirPath)
  }

  fun checkout(project: Project) {
    execGitCmd(project, "checkout", "--quiet", branch)
  }

  fun pull(project: Project) {
    execGitCmd(project, "pull", "--quiet", "--recurse-submodules", "--rebase")
  }


  override fun toString(): String {
    return String.format("  %1$-30s : update = %2$-5s, branch = %3$-20s, path = %4$-30s, url = %5\$s", name, update, branch, dirPath, url)
  }
}

data class RepoConfig(val name: String, val defaultUpdate: Boolean?, val defaultUrl: String?, val defaultBranch: String?, val defaultDirPath: String?)

@Suppress("unused")
open class DevenvExtension(private val project: Project) {
  internal val repos = mutableListOf<RepoConfig>()

  var repoUrlPrefix: String? = null

  @JvmOverloads
  fun registerRepo(name: String, defaultUpdate: Boolean? = null, defaultUrl: String? = null, defaultBranch: String? = null, defaultDirPath: String? = null) {
    repos.add(RepoConfig(name, defaultUpdate, defaultUrl, defaultBranch, defaultDirPath))
  }

  fun registerCompositeBuildTask(name: String, description: String) {
    project.tasks.register(name) {
      this.group = "composite build"
      this.description = description
      this.dependsOn(project.gradle.includedBuilds.map { it.task(":$name") })
    }
  }
}
