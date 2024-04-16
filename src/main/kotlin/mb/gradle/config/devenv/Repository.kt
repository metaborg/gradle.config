package mb.gradle.config.devenv

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.regex.Pattern

/** Specifies a Git transport mode. */
enum class Transport {
    /** Use the SSH protocol. */
    SSH,

    /** Use the HTTPS protocol. */
    HTTPS,
    ;

    private val httpsPattern = Pattern.compile("https?://([\\w.@:\\-~]+)/(.+)")
    private val sshPattern = Pattern.compile("(?:ssh://)?([\\w.@\\-~]+)@([\\w.@\\-~]+)[:/](.+)")

    /**
     * Changes the specified Git repository URL to use this transport protocol.
     *
     * @param url the URL to change
     * @return the changed url
     */
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

            else -> error("Cannot convert URL '$url' to '$this' format; unknown URL format.")
        }
        return when (this) {
            SSH -> "$user@$host:$path"
            HTTPS -> "https://$host/$path"
        }
    }
}

/** A Git root repository. */
data class RootRepository(
    /** The directory of the repository. */
    val rootDirectory: File,
    /** The branch of the repository to use; or `null` to not specify. */
    val rootBranch: String?,
    /** The URL prefix to use. */
    val urlPrefix: String,
    /** A map of names to sub repositories. */
    val repositories: Map<String, Repository>
) {
    companion object {
        /**
         * Represents the specified root directory as a root repository.
         *
         * @param rootDirectory the root directory
         * @return the root repository
         */
        fun fromRootDirectory(rootDirectory: File): RootRepository {
            val repoConfigs = RepositoryConfigurations.fromRootDirectory(rootDirectory)
            val rootBranch = Git.getCurrentBranch(rootDirectory)
            return fromRepoConfigs(rootDirectory, rootBranch, repoConfigs)
        }

        /**
         * Represents the specified root directory, branch, and sub repository configurations
         * as a root repository.
         *
         * @param rootDirectory the root directory
         * @param rootBranch the branch of the root repository
         * @param repositoryConfigurations the sub repository configurations
         */
        private fun fromRepoConfigs(
            rootDirectory: File,
            rootBranch: String?,
            repositoryConfigurations: RepositoryConfigurations
        ): RootRepository {
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

    /**
     * Determines if the repository is present and (in case of a submodule) initialized.
     *
     * @return `true` if the repository is checked-out and initialized;
     * otherwise, `false`
     */
    fun isCheckedOut(rootProject: Project, repo: Repository): Boolean =
        repo.repoDir(rootProject).exists() && if (repo.submodule) !submoduleStatus(
            rootProject,
            repo
        ).startsWith("-") else true

    fun add(rootProject: Project, repo: Repository) {
        rootProject.gitExec("add", "--", repo.directory)
    }

    fun commit(rootProject: Project, message: String) {
        rootProject.gitExec("commit", "--quiet", "--message", message)
    }

    fun submoduleInit(rootProject: Project, repo: Repository) {
        rootProject.gitExec("submodule", "update", "--quiet", "--recursive", "--init", "--", repo.directory)
    }

    fun submoduleUpdate(rootProject: Project, repo: Repository) {
        rootProject.gitExec("submodule", "update", "--quiet", "--recursive", "--", repo.directory)
    }

    fun clone(rootProject: Project, repo: Repository, transport: Transport) {
        val branch = repo.branch
            ?: throw GradleException("Cannot clone ${repo.fancyName}, no branch is set and root repository is not on a branch.")
        rootProject.gitExec(
            "clone",
            "--quiet",
            "--recurse-submodules",
            "--branch",
            branch,
            transport.convert(repo.url),
            repo.directory
        )
    }

    fun submoduleStatus(rootProject: Project, repo: Repository): String {
        return rootProject.gitExec(
            "submodule",
            "status",
            "--",
            repo.directory,
            printCommandLine = false,
            captureOutput = true
        ).trim()
    }

    /**
     * Attempts to execute the specified Git command in the root directory,
     * optionally capturing the standard output.
     *
     * @param args the arguments to the `git` command
     * @param printCommandLine whether to print the command line being executed
     * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
     * @return the captured standard output; or an empty string if [captureOutput] is `false`; or `null` if the command failed
     */
    private fun Project.gitMaybeExec(
        vararg args: String,
        printCommandLine: Boolean = true,
        captureOutput: Boolean = false
    ): String? = Git.maybeExec(
        this,
        null,
        *args,
        printCommandLine = printCommandLine,
        captureOutput = captureOutput
    )

    /**
     * Executes the specified Git command in the root directory, asserting that the command succeeded
     * and optionally capturing the standard output.
     *
     * @param args the arguments to the `git` command
     * @param printCommandLine whether to print the command line being executed
     * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
     * @return the captured standard output; or an empty string if [captureOutput] is `false`
     */
    private fun Project.gitExec(
        vararg args: String,
        printCommandLine: Boolean = true,
        captureOutput: Boolean = false
    ): String = Git.exec(
        this,
        null,
        *args,
        printCommandLine = printCommandLine,
        captureOutput = captureOutput
    )
}

/** A Git sub repository. */
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

    fun repoDir(rootProject: Project): File = rootProject.projectDir.resolve(directory)


    fun status(rootProject: Project, short: Boolean = false) {
        rootProject.gitExec(
            "-c",
            "color.status=always",
            "status",
            "--branch",
            if (short) "--short" else "",
            printCommandLine = false
        )
    }

    fun fetch(rootProject: Project) {
        rootProject.gitExec("fetch", "--quiet", "--recurse-submodules", "--all")
    }

    fun checkout(rootProject: Project) {
        val branch = branch
            ?: throw GradleException("Cannot switch $fancyName, no branch is set and root repository is not on a branch.")
        rootProject.gitExec("switch", "--quiet", "--", branch)
    }

    fun pull(rootProject: Project) {
        rootProject.gitExec("pull", "--quiet", "--recurse-submodules", "--rebase", "--autostash")
    }

    fun push(rootProject: Project, all: Boolean = false, followTags: Boolean = false) {
        rootProject.gitExec(
            "push",
            if (all) "--all" else "",
            if (followTags) "--follow-tags" else ""
        )
    }

    fun clean(rootProject: Project, dryRun: Boolean, removeIgnored: Boolean = false) {
        // -X: remove untracked files
        // -x: remove untracked and ignored untracked files
        // -d: remove untracked directories
        rootProject.gitExec(
            "clean", "--force", if (removeIgnored) "-x" else "-X", "-d",
            if (dryRun) "--dry-run" else ""
        )
    }

    fun reset(rootProject: Project, hard: Boolean) {
        val branch = branch
            ?: throw GradleException("Cannot reset $fancyName, no branch is set and root repository is not on a branch.")
        rootProject.gitExec("reset", branch, if (hard) "--hard" else "--mixed")
    }

    /** Prints the current commit and log message of this repository. */
    fun printCommit(rootProject: Project) {
        rootProject.gitMaybeExec("log", "--decorate", "--oneline", "-1", printCommandLine = false)
    }

    /** Gets the current commit hash of this repository; or `null` if it could not be determined. */
    fun getCommit(rootProject: Project): String? {
        return rootProject.gitMaybeExec(
            "rev-parse",
            "--verify",
            "HEAD",
            printCommandLine = false,
            captureOutput = true
        )?.trim()
    }

    /** Gets the default remote name of the repository; or `null` if it could not be determined. */
    fun getDefaultRemote(rootProject: Project): String? {
        return rootProject.gitMaybeExec(
            "config",
            "--get",
            "checkout.defaultRemote",
            printCommandLine = false,
            captureOutput = true
        )?.takeIf { it.isNotBlank() }?.trim()
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
        val branch = branch
            ?: throw GradleException("Cannot fix branch of $fancyName, no branch is set and root repository is not on a branch.")
        val remote = getRemote(rootProject)
        // Ensure we are in detached HEAD mode
        rootProject.gitExec("checkout", "--quiet", "--detach")
        // Force the branch to point to our detached HEAD
        rootProject.gitExec("branch", "--quiet", "--force", "--", branch)
        // Fix the upstream remote of the branch
        rootProject.gitExec("branch", "--quiet", "--set-upstream-to=$remote/$branch", "--", branch)
        // Checkout the branch, reattaching the HEAD
        checkout(rootProject)
    }

    val fancyName: String get() = if (submodule) "submodule $name" else "repository $name"

    val typeStr: String get() = if (submodule) "submodule" else "repository"

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


    /**
     * Attempts to execute the specified Git command in the subdirectory of the repository,
     * optionally capturing the standard output.
     *
     * @param args the arguments to the `git` command
     * @param printCommandLine whether to print the command line being executed
     * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
     * @return the captured standard output; or an empty string if [captureOutput] is `false`; or `null` if the command failed
     */
    private fun Project.gitMaybeExec(
        vararg args: String,
        printCommandLine: Boolean = true,
        captureOutput: Boolean = false
    ): String? = Git.maybeExec(
        this,
        repoDir(this),
        *args,
        printCommandLine = printCommandLine,
        captureOutput = captureOutput
    )

    /**
     * Executes the specified Git command in the subdirectory of the repository, asserting that the command succeeded
     * and optionally capturing the standard output.
     *
     * @param args the arguments to the `git` command
     * @param printCommandLine whether to print the command line being executed
     * @param captureOutput `true` to capture the standard output; otherwise, `false` to print it to `System.out`
     * @return the captured standard output; or an empty string if [captureOutput] is `false`
     */
    private fun Project.gitExec(
        vararg args: String,
        printCommandLine: Boolean = true,
        captureOutput: Boolean = false
    ): String = Git.exec(
        this,
        repoDir(this),
        *args,
        printCommandLine = printCommandLine,
        captureOutput = captureOutput
    )
}

