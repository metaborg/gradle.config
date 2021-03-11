package mb.gradle.config.devenv

import org.gradle.api.Project
import java.io.File

data class Repositories(
  val rootDirectory: File,
  val rootBranch: String,
  val urlPrefix: String,
  val repositories: Map<String, Repository>
) {
  companion object {
    fun fromRootDirectory(rootDirectory: File): Repositories {
      val repoConfigs = RepositoryConfigurations.fromRootDirectory(rootDirectory)
      val rootBranch = getCurrentGitBranch(rootDirectory)
      return fromRepoConfigs(rootDirectory, rootBranch, repoConfigs)
    }

    private fun fromRepoConfigs(rootDirectory: File, rootBranch: String, repositoryConfigurations: RepositoryConfigurations): Repositories {
      val repos = repositoryConfigurations.configurations.mapValues { (name, repoConfig) ->
        val include = repoConfig.include
        val update = repoConfig.update
        val directory = repoConfig.directory
        val url = repoConfig.url ?: "${repositoryConfigurations.urlPrefix}/$name.git"
        val branch = repoConfig.branch ?: rootBranch
        Repository(name, include, update, directory, url, branch)
      }
      return Repositories(rootDirectory, rootBranch, repositoryConfigurations.urlPrefix, repos)
    }
  }
}

class Repository(
  val name: String,
  val include: Boolean,
  val update: Boolean,
  val directory: String,
  val url: String,
  val branch: String
) {
  fun dir(rootDir: File) = rootDir.resolve(directory)

  fun isCheckedOut(rootDir: File) = dir(rootDir).exists()


  fun execGitCmd(rootProject: Project, vararg args: String, printCommandLine: Boolean = true) {
    execGitCmd(rootProject, mutableListOf(*args), printCommandLine)
  }

  fun execGitCmd(rootProject: Project, args: MutableList<String>, printCommandLine: Boolean = true) {
    val dir = dir(rootProject.projectDir)
    if(!dir.exists()) {
      error("Cannot execute git command in directory $dir, directory does not exist")
    }
    rootProject.exec {
      this.executable = "git"
      this.workingDir = dir
      this.args = args.filterNot { it.isBlank() }
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
      this.args = args.filterNot { it.isBlank() }
      if(printCommandLine) {
        println(commandLine.joinToString(separator = " "))
      }
    }
  }


  fun status(rootProject: Project, short: Boolean = false) {
    execGitCmd(rootProject, "-c", "color.status=always", "status", if (short) "-sb" else "", printCommandLine = false)
  }

  fun clone(rootProject: Project) {
    execGitCmdInRoot(rootProject, "clone", "--quiet", "--recurse-submodules", "--branch", branch, url, directory)
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

  fun pushTags(rootProject: Project) {
    execGitCmd(rootProject, "push", "--follow-tags")
  }

  fun pushAll(rootProject: Project) {
    execGitCmd(rootProject, "push", "--all")
  }

  fun pushAllTags(rootProject: Project) {
    execGitCmd(rootProject, "push", "--all", "--follow-tags")
  }

  fun clean(rootProject: Project, dryRun: Boolean, removeIgnored: Boolean = false) {
    // -X: remove untracked files
    // -x: remove untracked and ignored untracked files
    // -d: remove untracked directories
    execGitCmd(rootProject, "clean", "--force", if (removeIgnored) "-x" else "-X", "-d",
      if (dryRun) "--dry-run" else "")
  }

  fun info() =
    String.format("  %1$-30s : include = %2$-5s, update = %3$-5s, branch = %4$-20s, path = %5$-30s, url = %6\$s", name, include, update, branch, directory, url)

  override fun toString() = name
}
