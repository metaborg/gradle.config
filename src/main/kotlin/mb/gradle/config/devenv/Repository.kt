package mb.gradle.config.devenv

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException
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
  val rootBranch: String?,
  val urlPrefix: String,
  val repositories: Map<String, Repository>
) {
  companion object {
    fun fromRootDirectory(rootDirectory: File): RootRepository {
      val repoConfigs = RepositoryConfigurations.fromRootDirectory(rootDirectory)
      val rootBranch = getCurrentGitBranch(rootDirectory)
      return fromRepoConfigs(rootDirectory, rootBranch, repoConfigs)
    }

    private fun fromRepoConfigs(rootDirectory: File, rootBranch: String?, repositoryConfigurations: RepositoryConfigurations): RootRepository {
      val repos = repositoryConfigurations.configurations.mapValues { (name, repoConfig) ->
        val include = repoConfig.include
        val update = repoConfig.update
        val directory = repoConfig.directory
        val url = repoConfig.url ?: "${repositoryConfigurations.urlPrefix}/$name.git"
        val branch = repoConfig.branch ?: rootBranch
        val remote = repoConfig.remote
        val submodule = repoConfig.submodule
        Repository(name, include, update, directory, url, branch, remote, submodule)
      }
      return RootRepository(rootDirectory, rootBranch, repositoryConfigurations.urlPrefix, repos)
    }
  }
}

/** A Git repository. */
data class Repository(
  /** The name of the repository, for debugging purposes. */
  val name: String,
  /** Whether the repository is included in the build. */
  val include: Boolean,
  /** Whether to modify the repository as part of this plugin's tasks. */
  val update: Boolean,
  /** The directory of the repository, relative to the root directory. */
  val directory: String,
  /** The URL of the repository to clone from. */
  val url: String,
  /** The branch of the repository to use; or `null` to use the root repository's branch. */
  val branch: String?,
  /** The remote name, say `"origin"`; or `null` to use the repository's default (`checkout.defaultRemote`). */
  val remote: String?,
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
   * @param root whether to execute the command in the root directory instead of the subdirectory
   * @return the captured standard output; or an empty string if [captureOutput] is `false`
   */
  fun execGitCmd(rootProject: Project, vararg args: String, printCommandLine: Boolean = true, captureOutput: Boolean = false, root: Boolean = false): String {
    (if (captureOutput) ByteArrayOutputStream() else null).use { stream ->
      val result = internalExecGitCmd(rootProject, mutableListOf(*args), printCommandLine, root = root, standardOutput = stream)
      result.rethrowFailure()
      result.assertNormalExitValue()
      return stream?.let { stream.toString("UTF-8") } ?: ""
    }
  }

  /**
   * Attempts to execute the specified Git command in the subdirectory of the repository,
   * optionally capturing the standard output.
   *
   * @param rootProject the Gradle project
   * @param args the arguments to the `git` command
   * @param printCommandLine whether to print the command line being executed
   * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
   * @param root whether to execute the command in the root directory instead of the subdirectory
   * @return the captured standard output; or an empty string if [captureOutput] is `false`; or `null` if the command failed
   */
  fun maybeExecGitCmd(rootProject: Project, vararg args: String, printCommandLine: Boolean = true, captureOutput: Boolean = false, root: Boolean = false): String? {
    try {
      return execGitCmd(rootProject, *args, printCommandLine = printCommandLine, captureOutput = captureOutput, root = root)
    } catch (ex: ExecException) {
      return null
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
    if (!root && !dir.exists()) return ExecResultImpl("git", -255, ExecException("Cannot execute git command in directory $dir, directory does not exist"))
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
    val branch = branch ?: throw GradleException("Cannot clone $fancyName, no branch is set and root repository is not on a branch.")
    execGitCmd(rootProject, "clone", "--quiet", "--recurse-submodules", "--branch", branch, transport.convert(url), directory, root = true)
  }

  fun submoduleStatus(rootProject: Project): String {
    return execGitCmd(rootProject, "submodule", "status", "--", directory, printCommandLine = false, captureOutput = true, root = true).trim()
  }

  fun submoduleInit(rootProject: Project) {
    execGitCmd(rootProject, "submodule", "update", "--quiet", "--recursive", "--init", "--", directory, root = true)
  }

  fun submoduleUpdate(rootProject: Project) {
    execGitCmd(rootProject, "submodule", "update", "--quiet", "--recursive", "--", directory, root = true)
  }

  fun fetch(rootProject: Project) {
    execGitCmd(rootProject, "fetch", "--quiet", "--recurse-submodules", "--all")
  }

  fun checkout(rootProject: Project) {
    val branch = branch ?: throw GradleException("Cannot switch $fancyName, no branch is set and root repository is not on a branch.")
    execGitCmd(rootProject, "switch", "--quiet", "--", branch)
  }

  fun addCommit(rootProject: Project) {
    execGitCmd(rootProject, "add", "--quiet", "--", directory, root = true)
  }

  fun commitRoot(rootProject: Project, message: String) {
    execGitCmd(rootProject, "commit", "--quiet", "--message", message, root = true)
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
    val branch = branch ?: throw GradleException("Cannot reset $fancyName, no branch is set and root repository is not on a branch.")
    execGitCmd(rootProject, "reset", branch, if(hard) "--hard" else "--mixed")
  }

  /** Prints the current commit and log message of this repository. */
  fun printCommit(rootProject: Project) {
    maybeExecGitCmd(rootProject, "log", "--decorate", "--oneline", "-1", printCommandLine = false)
  }

  /** Gets the current commit hash of this repository; or `null` if it could not be determined. */
  fun getCommit(rootProject: Project): String? {
    return maybeExecGitCmd(rootProject, "rev-parse", "--verify", "HEAD", printCommandLine = false, captureOutput = true)?.trim()
  }

  /** Gets the default remote name of the repository; or `null` if it could not be determined. */
  fun getDefaultRemote(rootProject: Project): String? {
    return maybeExecGitCmd(rootProject, "config", "--get", "checkout.defaultRemote", printCommandLine = false, captureOutput = true)?.takeIf { it.isNotBlank() }?.trim()
  }

  /** Gets the name of the remote to use. Defaults to `"origin"` if it cannot be determined. */
  fun getRemote(rootProject: Project): String {
    return remote ?: getDefaultRemote(rootProject) ?: "origin"
  }

  /**
   * Fixes the detached head on a submodule by moving the branch pointer to the current commit
   * and checking out the branch.
   */
  fun fixBranch(rootProject: Project) {
    val branch = branch ?: throw GradleException("Cannot fix branch of $fancyName, no branch is set and root repository is not on a branch.")
    val remote = getRemote(rootProject)
    // Ensure we are in detached HEAD mode
    execGitCmd(rootProject, "checkout", "--quiet", "--detach")
    // Force the branch to point to our detached HEAD
    execGitCmd(rootProject, "branch", "--quiet", "--force", "--", branch)
    // Fix the upstream remote of the branch
    execGitCmd(rootProject, "branch", "--quiet", "--set-upstream-to=$remote/$branch", "--", branch)
    // Checkout the branch, reattaching the HEAD
    checkout(rootProject)
  }

  val fancyName: String get() = if (submodule) "submodule $name" else "repository $name"

  fun info() = String.format(
    "  %1\$-30s : include = %2\$-5s, update = %3\$-5s, submodule = %4\$-5s, branch = %5\$-20s, remote = %6\$-20s, path = %7\$-30s, url = %8\$s",
    name,
    include,
    update,
    submodule,
    branch,
    remote,
    directory,
    url
  )

  override fun toString() = name
}


private class ExecResultImpl(
  private val displayName: String,
  private val exitValue: Int,
  private val failure: ExecException? = null
) : ExecResult {
  override fun getExitValue(): Int = exitValue

  override fun assertNormalExitValue(): ExecResult {
    if (exitValue != 0) throw ExecException("Process '$displayName' finished with non-zero exit value $exitValue")
    return this
  }

  override fun rethrowFailure(): ExecResult {
    if (failure != null) throw failure
    return this
  }

  override fun toString(): String {
    return "{exitValue=$exitValue, failure=$failure}"
  }
}
