package mb.gradle.config.devenv

import org.eclipse.jgit.lib.internal.WorkQueue
import org.gradle.api.DefaultTask
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
    val repositories = Repositories.fromRootDirectory(project.rootDir)
    project.tasks.register<RepositoryTask>("list") {
      doLast {
        println("Git URL prefix: ${repositories.urlPrefix}")
        println("Current branch: ${repositories.rootBranch}")
        println("Repositories:")
        for(repo in repositories.repositories.values) {
          println(repo)
        }
      }
      description = "Lists the repositories and their properties."
    }
    project.tasks.register<StatusRepositoryTask>("status") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Status for repository $repo:")
          repo.status(project, short)
          println()
        }
      }
      description = "For each repository (with update=true): show its status."
    }
    project.tasks.register<RepositoryTask>("clone") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          if(repo.isCheckedOut(project.rootDir)) continue
          println("Cloning repository $repo:")
          repo.clone(project, transport)
          println()
        }
      }
      description = "For each repository (with update=true): clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("fetch") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Fetching for repository $repo:")
          repo.fetch(project)
          println()
        }
      }
      description = "For each repository (with update=true): fetch from the main remote."
    }
    project.tasks.register<RepositoryTask>("checkout") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Checking out ${repo.branch} for repository $repo:")
          repo.checkout(project)
          println()
        }
      }
      description = "For each repository (with update=true): checkout the correct branch."
    }
    project.tasks.register<RepositoryTask>("update") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          if(!repo.isCheckedOut(project.rootDir)) {
            println("Cloning repository $repo:")
            repo.clone(project, transport)
          } else {
            println("Updating repository $repo:")
            repo.fetch(project)
            repo.checkout(project)
            repo.pull(project)
          }
          println()
        }
      }
      description = "For each repository (with update=true): check out the repository to the correct branch and pull from origin, or clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("push") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing current branch for repository $repo:")
          repo.push(project)
          println()
        }
      }
      description = "For each repository (with update=true): push the current local branch to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushTags") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing current branch and annotated tags for repository $repo:")
          repo.pushTags(project)
          println()
        }
      }
      description = "For each repository (with update=true): push the current local branch and annotated tags to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushAll") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing all branches for repository $repo:")
          repo.pushAll(project)
          println()
        }
      }
      description = "For each repository (with update=true): push all local branches to the main remote."
    }
    project.tasks.register<RepositoryTask>("pushAllTags") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing all branches and annotated tags for repository $repo:")
          repo.pushAllTags(project)
          println()
        }
      }
      description = "For each repository (with update=true): push all local branches and annotated tags to the main remote."
    }
    project.tasks.register<CleanRepositoryTask>("clean") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Cleaning repository $repo:")
          repo.clean(project, !force, removeIgnored)
          println()
        }
      }
      description = "For each repository (with update=true): remove untracked files and directories."
    }

    // Shutdown JGit work queue after build is finished to free resources.
    project.gradle.buildFinished {
      WorkQueue.getExecutor().shutdown()
    }
  }
}


open class RepositoryTask : DefaultTask() {
  @Input
  @Option(option = "transport", description = "Transport protocol to use. Defaults to SSH.")
  var transport: Transport = Transport.SSH

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

open class StatusRepositoryTask : RepositoryTask() {
  @Input
  @Option(option = "short", description = "Print short status info.")
  var short: Boolean = false
}
