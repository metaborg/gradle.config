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
    val rootRepository = RootRepository.fromRootDirectory(project.rootDir)
    project.tasks.register<RepositoryTask>("list") {
      doLast {
        println("Root repository:")
        println("  Git URL prefix: ${rootRepository.urlPrefix}")
        println("  Current branch: ${rootRepository.rootBranch ?: "<unknown>"}")
        val allRepositories = rootRepository.repositories.values
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
        val selected = getSelectedRepositories(rootRepository, allowNotUpdated = true)
        for(repo in selected) {
          println("Status for ${repo.fancyName}:")
          repo.printCommit(project)
          if (!repo.update) {
            println("Not updated ${repo.fancyName}")
          } else if(repo.isCheckedOut(project)) {
            repo.status(project, short)
          } else {
            println("Not cloned ${repo.fancyName}")
          }
          println()
        }
      }
      description = "For each repository (with update=true): show its status."
    }
    project.tasks.register<RepositoryTask>("clone") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(repo.isCheckedOut(project)) {
            println("Already cloned ${repo.fancyName}.")
          } else if (repo.submodule) {
            println("Initializing ${repo.fancyName}:")
            repo.submoduleInit(project)
            repo.fixBranch(project)
          } else {
            println("Cloning ${repo.fancyName}:")
            repo.clone(project, transport)
          }
          repo.printCommit(project)
          println()
        }
      }
      description = "For each repository (with update=true): clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("update") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            if (repo.submodule) {
              println("Initializing and updating ${repo.fancyName}:")
              repo.submoduleInit(project)
              repo.fixBranch(project)
              repo.pull(project)
            } else {
              println("Cloning ${repo.fancyName}:")
              repo.clone(project, transport)
            }
            repo.printCommit(project)
          } else {
            println("Updating ${repo.fancyName}:")
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
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Fetching for ${repo.fancyName}:")
            repo.fetch(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): fetch from the main remote."
    }
    project.tasks.register<RepositoryTask>("checkout") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Checking out ${repo.branch} for ${repo.fancyName}:")
            if (repo.submodule) {
              repo.submoduleUpdate(project)
              repo.fixBranch(project)
            } else {
              repo.checkout(project)
            }
            repo.printCommit(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): checkout the current commit on the correct branch."
    }
    project.tasks.register<RepositoryTask>("update") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            if (repo.submodule) {
              println("Initializing ${repo.fancyName}:")
              repo.submoduleInit(project)
              repo.fixBranch(project)
            } else {
              println("Cloning ${repo.fancyName}:")
              repo.clone(project, transport)
            }
            repo.printCommit(project)
          } else {
            println("Updating ${repo.fancyName}:")
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
    project.tasks.register<RepositoryTask>("push") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Pushing current branch for ${repo.fancyName}:")
            repo.push(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): push the current local branch to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushTags") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Pushing current branch and annotated tags for ${repo.fancyName}:")
            repo.pushTags(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): push the current local branch and annotated tags to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushAll") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Pushing all branches for ${repo.fancyName}:")
            repo.pushAll(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): push all local branches to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushAllTags") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Pushing all branches and annotated tags for ${repo.fancyName}:")
            repo.pushAllTags(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): push all local branches and annotated tags to the main remote."
    }
    project.tasks.register<CleanRepositoryTask>("clean") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Cleaning ${repo.fancyName}:")
            repo.clean(project, !force, removeIgnored)
          }
          println()
        }
      }
      description = "For each repository (with update=true): remove untracked files and directories."
    }
    project.tasks.register<ResetRepositoryTask>("reset") {
      doLast {
        val selected = getSelectedRepositories(rootRepository)
        for(repo in selected.filter { it.update }) {
          if(!repo.isCheckedOut(project)) {
            println("Not cloned ${repo.fancyName}")
          } else {
            println("Resetting ${repo.fancyName}:")
            repo.reset(project, hard)
          }
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

open class CleanRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "force", description = "Performs the clean without a dry-run.")
  var force: Boolean = false

  @Input
  @Option(option = "removeIgnored", description = "Also remove ignored untracked files.")
  var removeIgnored: Boolean = false
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
