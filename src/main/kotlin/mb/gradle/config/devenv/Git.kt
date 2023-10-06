package mb.gradle.config.devenv

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream


/** Git command-line functions. */
object Git {

  /**
   * Attempts to execute the specified Git command in the specified working directory,
   * optionally capturing the standard output.
   *
   * @param rootProject the Gradle project
   * @param workingDir the working directory in which to execute the command; or `null` to use the Gradle project directory
   * @param args the arguments to the `git` command
   * @param printCommandLine whether to print the command line being executed
   * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
   * @return the captured standard output; or an empty string if [captureOutput] is `false`; or `null` if the command failed
   */
  fun maybeExec(
    rootProject: Project,
    workingDir: File?,
    vararg args: String,
    printCommandLine: Boolean = true,
    captureOutput: Boolean = false
  ): String? {
    return try {
      exec(
        rootProject,
        workingDir,
        *args,
        printCommandLine = printCommandLine,
        captureOutput = captureOutput
      )
    } catch (ex: ExecException) {
      null
    }
  }

  /**
   * Executes the specified Git command in the specified working directory, asserting that the command succeeded
   * and optionally capturing the standard output.
   *
   * @param rootProject the Gradle project
   * @param workingDir the working directory in which to execute the command; or `null` to use the Gradle project directory
   * @param args the arguments to the `git` command
   * @param printCommandLine whether to print the command line being executed
   * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
   * @return the captured standard output; or an empty string if [captureOutput] is `false`
   */
  fun exec(
    rootProject: Project,
    workingDir: File?,
    vararg args: String,
    printCommandLine: Boolean = true,
    captureOutput: Boolean = false
  ): String {
    (if (captureOutput) ByteArrayOutputStream() else null).use { stream ->
      val result = internalExec(
        rootProject,
        workingDir,
        mutableListOf(*args),
        printCommandLine,
        standardOutput = stream
      )
      result.rethrowFailure()
      result.assertNormalExitValue()
      return stream?.let { stream.toString("UTF-8") } ?: ""
    }
  }

  /**
   * Executes a Git command.
   *
   * @param rootProject the Gradle project
   * @param workingDir the working directory in which to execute the command; or `null` to use the Gradle project directory
   * @param args the command-line arguments to the Git command
   * @param printCommandLine whether to print the command line being executed
   * @param standardOutput the standard output stream to use; or `null` to use the default
   */
  private fun internalExec(
    rootProject: Project,
    workingDir: File?,
    args: MutableList<String>,
    printCommandLine: Boolean = true,
    standardOutput: OutputStream? = null
  ): ExecResult {
    if (workingDir != null && !workingDir.exists()) return ExecResultImpl("git", -255, ExecException("Cannot execute git command in directory $workingDir, directory does not exist"))
    val result = rootProject.exec {
      this.executable = "git"
      this.args = args.filterNot { it.isBlank() }
      workingDir?.let { this.workingDir = it }
      standardOutput?.let { this.standardOutput = it }
      if(printCommandLine) println(commandLine.joinToString(separator = " "))
    }
    return result
  }

  /**
   * Gets the current Git branch name of the repository at the specified directory.
   *
   * @param directory the directory for the repository
   * @return the current Git branch name; or `null` when it is not symbolic
   */
  fun getCurrentBranch(directory: File): String? {
    try {
      FileRepositoryBuilder().readEnvironment().findGitDir(directory).setMustExist(true).build()
    } catch (e: RepositoryNotFoundException) {
      throw GradleException("Cannot retrieve current branch name because no git repository was found at '$directory'", e)
    }.use { repo ->
      // Use repository with 'use' to close repository after use, freeing up resources.
      val headRef = repo.exactRef(Constants.HEAD)
        ?: throw GradleException("Cannot retrieve current branch name because repository has no HEAD")
      if (!headRef.isSymbolic) {
        println("Warning: root project is not on a symbolic branch, but on '${headRef.target.name}'")
        return null
      }
      return Repository.shortenRefName(headRef.target.name)
    }
  }


  /** The result of executing a command. */
  private class ExecResultImpl(
    /** The display name of the command. */
    private val displayName: String,
    /** The exit value of the command. */
    private val exitValue: Int,
    /** The execution exception, if any; otherwise, `null`. */
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
      return if (failure != null) {
        "{exitValue=$exitValue, failure=$failure}"
      } else {
        "{exitValue=$exitValue}"
      }
    }
  }

}
