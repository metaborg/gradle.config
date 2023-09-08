package mb.gradle.config.devenv

import org.gradle.api.Project
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.util.regex.Pattern

enum class Transport {
  SSH, HTTPS;

  private val httpsPattern = Pattern.compile("https?://([\\w.@:\\-~]+)/(.+)")
  private val sshPattern = Pattern.compile("(?:ssh://)?([\\w.@\\-~]+)@([\\w.@\\-~]+)[:/](.+)")

  fun convert(url: String): String {
    val httpsMatch = httpsPattern.matcher(url)
    val sshMatch = sshPattern.matcher(url)
    val user: String
    val host: String
    val path: String
    when {
      httpsMatch.matches() -> {
        user = "git"
        host = httpsMatch.group(1)
        path = httpsMatch.group(2)
      }
      sshMatch.matches() -> {
        user = sshMatch.group(1)
        host = sshMatch.group(2)
        path = sshMatch.group(3)
      }
      else -> {
        throw RuntimeException("Cannot convert URL '$url' to '$this' format; unknown URL format")
      }
    }
    return when(this) {
      SSH -> "$user@$host:$path"
      HTTPS -> "https://$host/$path"
    }
  }
}

data class RootRepository(
  val rootDirectory: File,
  val rootBranch: String,
  val urlPrefix: String,
  val repositories: Map<String, Repository>
) {
  companion object {
    fun fromRootDirectory(rootDirectory: File): RootRepository {
      val repoConfigs = RepositoryConfigurations.fromRootDirectory(rootDirectory)
      val rootBranch = getCurrentGitBranch(rootDirectory)
      return fromRepoConfigs(rootDirectory, rootBranch, repoConfigs)
    }

    private fun fromRepoConfigs(rootDirectory: File, rootBranch: String, repositoryConfigurations: RepositoryConfigurations): RootRepository {
      val repos = repositoryConfigurations.configurations.mapValues { (name, repoConfig) ->
        val include = repoConfig.include
        val update = repoConfig.update
        val directory = repoConfig.directory
        val url = repoConfig.url ?: "${repositoryConfigurations.urlPrefix}/$name.git"
        val branch = repoConfig.branch ?: rootBranch
        val submodule = repoConfig.submodule
        Repository(name, include, update, directory, url, branch, submodule)
      }
      return RootRepository(rootDirectory, rootBranch, repositoryConfigurations.urlPrefix, repos)
    }
  }
}

class Repository(
  val name: String,
  val include: Boolean,
  val update: Boolean,
  val directory: String,
  val url: String,
  val branch: String,
  /** Whether the repository is a submodule. */
  val submodule: Boolean
) {
  private fun dir(rootDir: File): File = rootDir.resolve(directory)

  /**
   * Determines if the repository is present and (in case of a submodule) initialized
   */
  fun isCheckedOut(rootProject: Project): Boolean =
     dir(rootProject.rootDir).exists() && if (submodule) !submoduleStatus(rootProject).startsWith("-") else true

  /**
   * Executes the specified Git command in the subdirectory of the repository, asserting that the command succeeded
   * and optionally capturing the standard output.
   *
   * @param rootProject the Gradle project
   * @param args the arguments to the `git` command
   * @param printCommandLine whether to print the command line being executed
   * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
   * @return the captured standard output; or `null` if [captureOutput] is `false`
   */
  fun execGitCmd(rootProject: Project, vararg args: String, printCommandLine: Boolean = true, captureOutput: Boolean = false): String? {
    (if (captureOutput) ByteArrayOutputStream() else null).use { stream ->
      val result = internalExecGitCmd(rootProject, mutableListOf(*args), printCommandLine, root = false, standardOutput = stream)
      result.assertNormalExitValue()
      return stream?.let { stream.toString("UTF-8") }
    }
  }

  /**
   * Executes the specified Git command in the root directory of the project, asserting that the command succeeded
   * and optionally capturing the standard output.
   *
   * @param rootProject the Gradle project
   * @param args the arguments to the `git` command
   * @param printCommandLine whether to print the command line being executed
   * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
   * @return the captured standard output; or `null` if [captureOutput] is `false`
   */
  fun execGitCmdInRoot(rootProject: Project, vararg args: String, printCommandLine: Boolean = true, captureOutput: Boolean = false): String? {
    (if (captureOutput) ByteArrayOutputStream() else null).use { stream ->
      val result = internalExecGitCmd(rootProject, mutableListOf(*args), printCommandLine, root = true, standardOutput = stream)
      result.assertNormalExitValue()
      return stream?.let { stream.toString("UTF-8") }
    }
  }

  private fun internalExecGitCmd(
    rootProject: Project,
    args: MutableList<String>,
    printCommandLine: Boolean = true,
    standardOutput: OutputStream? = null,
    root: Boolean = false
  ): ExecResult {
    val dir = dir(rootProject.projectDir)
    if (!root) check(dir.exists()) { "Cannot execute git command in directory $dir, directory does not exist" }
    val result = rootProject.exec {
      this.executable = "git"
      if (!root) { this.workingDir = dir }
      this.args = args.filterNot { it.isBlank() }
      standardOutput?.let { this.standardOutput = it }
      if(printCommandLine) println(commandLine.joinToString(separator = " "))
    }
    return result
  }

  fun status(rootProject: Project, short: Boolean = false) {
    execGitCmd(rootProject, "-c", "color.status=always", "status", "--branch", if(short) "--short" else "", printCommandLine = false)
  }

  fun clone(rootProject: Project, transport: Transport) {
    execGitCmdInRoot(rootProject, "clone", "--quiet", "--recurse-submodules", "--branch", branch, transport.convert(url), directory)
  }

  fun submoduleStatus(rootProject: Project): String {
    return execGitCmdInRoot(rootProject, "submodule", "status", "--", directory, printCommandLine = false, captureOutput = true)!!
  }

  fun submoduleInit(rootProject: Project) {
    execGitCmdInRoot(rootProject, "submodule", "update", "--init", "--recursive", "--quiet", "--", directory)
  }

  fun fetch(rootProject: Project) {
    execGitCmd(rootProject, "fetch", "--quiet", "--recurse-submodules", "--all")
  }

  fun checkout(rootProject: Project) {
    execGitCmd(rootProject, "checkout", "--quiet", "--", branch)
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
    execGitCmd(rootProject, "clean", "--force", if(removeIgnored) "-x" else "-X", "-d",
      if(dryRun) "--dry-run" else "")
  }

  fun reset(rootProject: Project, hard: Boolean) {
    execGitCmd(rootProject, "reset", branch, if(hard) "--hard" else "--mixed")
  }

  /** Prints the current commit of this repository. */
  fun printCommit(rootProject: Project) {
    execGitCmd(rootProject, "rev-parse", "--verify", "HEAD", printCommandLine = false)
  }

  /**
   * Fixes the detached head on a submodule by moving the branch pointer to the current commit
   * and checking out the branch.
   */
  fun fixBranch(rootProject: Project) {
    // Ensure we are in detached HEAD mode
    execGitCmd(rootProject, "checkout", "--quiet", "--detach")
    // Force the branch to point to our detached HEAD
    execGitCmd(rootProject, "branch", "--force", "--quiet", "--", branch)
    // Checkout the branch, reattaching the HEAD
    checkout(rootProject)
  }

  val fancyName: String get() = if (submodule) "submodule $name" else "repository $name"

  fun info() =
    String.format("  %1\$-30s : include = %2\$-5s, update = %3\$-5s, submodule = %4\$-5s, branch = %5\$-20s, path = %6\$-30s, url = %7\$s", name, include, update, submodule, branch, directory, url)

  override fun toString() = name
}
