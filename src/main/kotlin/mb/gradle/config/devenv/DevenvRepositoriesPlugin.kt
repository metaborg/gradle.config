package mb.gradle.config.devenv

import org.eclipse.jgit.lib.internal.WorkQueue
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class DevenvRepositoriesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    configure(project)
  }

  private fun configure(project: Project) {
    val rootRepo = RootRepository.fromRootDirectory(project.rootDir)
    project.tasks.register<RepositoryTask>("list") {
      doLast {
        println("Root repository:")
        println("  Git URL prefix: ${rootRepo.urlPrefix}")
        println("  Current branch: ${rootRepo.rootBranch ?: "<unknown>"}")
        val allRepositories = rootRepo.repositories.values
        val repositories = allRepositories.filter { !it.submodule }
        if (repositories.isNotEmpty()) {
          println("Repositories:")
          for (repo in repositories) {
            println("  ${repo.name} (${repo.branch ?: "<unknown>"})")
          }
        }
        val submodules = allRepositories.filter { it.submodule }
        if (submodules.isNotEmpty()) {
          println("Submodules:")
          for (repo in submodules) {
            val commit = repo.getCommit(project)
            println("  ${repo.name} (${repo.branch ?: "<unknown branch>"}, ${commit ?: "<unknown commit>"})")
          }
        }
        if (repositories.isEmpty() && submodules.isEmpty()) {
          println("No repositories or submodules configured.")
        }
      }
      description = "Lists the repositories and their properties."
    }
    project.tasks.register<StatusRepositoryTask>("status") {
      doLast {
        val selected = getSelectedRepositories(rootRepo, allowNotUpdated = true)
        for(repo in selected) {
          if (!repo.update) {
            println("${repo.name}: ${repo.typeStr} not updated")
          } else if(!rootRepo.isCheckedOut(project, repo)) {
            println("${repo.name}: ${repo.typeStr} not cloned")
          } else {
            println("${repo.name} ${repo.typeStr} status:")
            repo.printCommit(project)
            repo.status(project, short)
            println()
          }
        }
      }
      description = "For each repository (with update=true): show its status."
    }
    project.tasks.register<RepositoryTask>("clone") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if(rootRepo.isCheckedOut(project, repo)) {
            println("${repo.name}: ${repo.typeStr} already cloned")
            continue
          }

          if (repo.submodule) {
            println("${repo.name}: initializing ${repo.typeStr}...")
            rootRepo.submoduleInit(project, repo)
            repo.fixBranch(project)
          } else {
            println("${repo.name}: cloning ${repo.typeStr}...")
            rootRepo.clone(project, repo, transport)
          }
          repo.printCommit(project)
          println()
        }
      }
      description = "For each repository (with update=true): clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("update") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if(!rootRepo.isCheckedOut(project, repo)) {
            if (repo.submodule) {
              println("${repo.name}: initializing and updating ${repo.typeStr}...")
              rootRepo.submoduleInit(project, repo)
              repo.fixBranch(project)
              repo.pull(project)
            } else {
              println("${repo.name}: cloning ${repo.typeStr}...")
              rootRepo.clone(project, repo, transport)
            }
            repo.printCommit(project)
          } else {
            println("${repo.name}: updating ${repo.typeStr}...")
            repo.fetch(project)
            repo.checkout(project)
            repo.pull(project)
            repo.printCommit(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): check out the repository to the correct branch and pull from origin, or clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("fetch") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue

          println("${repo.name}: fetching ${repo.typeStr}...")
          repo.fetch(project)
          println()
        }
      }
      description = "For each repository (with update=true): fetch from the main remote."
    }
    project.tasks.register<RepositoryTask>("checkout") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue

          println("${repo.name}: checking out ${repo.branch} for ${repo.typeStr}...")
          if (repo.submodule) {
            rootRepo.submoduleUpdate(project, repo)
            repo.fixBranch(project)
          } else {
            repo.checkout(project)
          }
          repo.printCommit(project)
          println()
        }
      }
      description = "For each repository (with update=true): checkout the current commit on the correct branch."
    }
    project.tasks.register<CommitSubmodulesTask>("commitSubmodules") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue
          if (!repo.submodule) {
            println("${repo.name}: ${repo.typeStr} is not a submodule")
            continue
          }

          println("${repo.name}: adding ${repo.typeStr} commit ${repo.getCommit(project) ?: "<unknown>"}...")
          rootRepo.add(project, repo)
          println()
        }
        rootRepo.commit(project, message) // Gets skipped if the commit is empty
        println("Created commit")
        println()
      }
      description = "Create a commit with the current commit of each repository (with update=true)."
    }
    project.tasks.register<PushRepositoryTask>("push") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue

          println("${repo.name}: pushing current branch of ${repo.typeStr}...")
          repo.push(project, all, followTags)
          println()
        }
      }
      description = "For each repository (with update=true): push the current local branch (and optionally tags) to the main remote."
    }
    project.tasks.register<CleanRepositoryTask>("clean") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue

          println("${repo.name}: cleaning ${repo.typeStr}...")
          repo.clean(project, !force, removeIgnored)
          println()
        }
      }
      description = "For each repository (with update=true): remove untracked files and directories."
    }
    project.tasks.register<ResetRepositoryTask>("reset") {
      doLast {
        val selected = getSelectedRepositories(rootRepo)
        for(repo in selected.filter { it.update }) {
          if (!project.ensureCloned(rootRepo, repo)) continue

          println("${repo.name}: resetting ${repo.typeStr}...")
          repo.reset(project, hard)
          println()
        }
      }
      description = "For each repository (with update=true): reset untracked files and directories."
    }

    // Shutdown JGit work queue after build is finished to free resources.
    project.gradle.buildFinished {
      WorkQueue.getExecutor().shutdown()
    }
  }

  private fun Project.ensureCloned(rootRepo: RootRepository, repo: Repository): Boolean {
    return if(!rootRepo.isCheckedOut(this, repo)) {
      println("${repo.name}: ${repo.typeStr} not cloned")
      false
    } else {
      true
    }
  }

  private fun RepositoryTask.getSelectedRepositories(rootRepository: RootRepository, allowNotUpdated: Boolean = false): Collection<Repository> {
    val selectedRepoNames = repos.takeIf { it.isNotEmpty() } ?: return rootRepository.repositories.values
    val selectedRepositories = rootRepository.repositories.filterKeys { key -> key in selectedRepoNames }.values
    val unknownRepositories = selectedRepoNames.filter { it !in rootRepository.repositories.keys }
    if (unknownRepositories.isNotEmpty()) {
      throw InvalidUserDataException("Unknown repositories: $unknownRepositories, only the following repositories are known: ${rootRepository.repositories.keys}")
    }
    val notUpdatedRepositories = selectedRepositories.filter { !it.update }
    if (!allowNotUpdated && notUpdatedRepositories.isNotEmpty()) {
      throw InvalidUserDataException("Cannot perform task on repositories that are not updated: $notUpdatedRepositories")
    }
    return selectedRepositories
  }
}


open class RepositoryTask : DefaultTask() {
  @Input
  @Option(option = "transport", description = "Transport protocol to use. Defaults to SSH.")
  var transport: Transport = Transport.SSH

  @Input
  @Option(option = "repo", description = "Repository to perform the task on. Defaults to all repositories.")
  var repos: List<String> = emptyList()

  init {
    group = "Devenv repository"
  }
}

open class CommitSubmodulesTask : RepositoryTask() {
  @Input
  @Option(option = "message", description = "Provide a commit message.")
  var message: String = "Update submodule revisions"
}

open class CleanRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "force", description = "Performs the clean without a dry-run.")
  var force: Boolean = false

  @Input
  @Option(option = "removeIgnored", description = "Also remove ignored untracked files.")
  var removeIgnored: Boolean = false
}

open class PushRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "all", description = "Push all branches.")
  var all: Boolean = false

  @Input
  @Option(option = "follow-tags", description = "Push all tags.")
  var followTags: Boolean = false
}

open class ResetRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "hard", description = "Performs a hard reset.")
  var hard: Boolean = false
}

open class StatusRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "short", description = "Print short status info.")
  var short: Boolean = false
}
