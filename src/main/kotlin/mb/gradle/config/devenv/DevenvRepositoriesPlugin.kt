package mb.gradle.config.devenv

import org.eclipse.jgit.lib.internal.WorkQueue
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class DevenvRepositoriesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    configure(project)
  }

  private fun configure(project: Project) {
    val repositories = Repositories.fromRootDirectory(project.rootDir)
    project.tasks.register<RepositoryTask>("repoList") {
      doLast {
        println("Git URL prefix: ${repositories.urlPrefix}")
        println("Current branch: ${repositories.rootBranch}")
        println("Repositories:")
        for(repo in repositories.repositories.values) {
          println(repo)
        }
      }
      description = "Lists the Git repositories of devenv and their properties."
    }
    project.tasks.register<RepositoryTask>("repoStatus") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Status for repository $repo:")
          repo.status(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: show its status"
    }
    project.tasks.register<RepositoryTask>("repoClone") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          if(repo.isCheckedOut(project.rootDir)) continue
          println("Cloning repository $repo:")
          repo.clone(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("repoFetch") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Fetching for repository $repo:")
          repo.fetch(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: fetch from the main remote."
    }
    project.tasks.register<RepositoryTask>("repoCheckout") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Checking out ${repo.branch} for repository $repo:")
          repo.checkout(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: checkout the correct branch."
    }
    project.tasks.register<RepositoryTask>("repoUpdate") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          if(!repo.isCheckedOut(project.rootDir)) {
            println("Cloning repository $repo:")
            repo.clone(project)
          } else {
            println("Updating repository $repo:")
            repo.checkout(project)
            repo.pull(project)
          }
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: check out the repository to the correct branch and pull from origin, or clone the repository if it has not been cloned yet."
    }
    project.tasks.register<RepositoryTask>("repoPush") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing current branch for repository $repo:")
          repo.push(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push the current local branch to the main remote."
    }
    project.tasks.register<RepositoryTask>("repoPushTags") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing current branch and annotated tags for repository $repo:")
          repo.pushTags(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push the current local branch and annotated tags to the main remote."
    }
    project.tasks.register<RepositoryTask>("repoPushAll") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing all branches for repository $repo:")
          repo.pushAll(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push all local branches to the main remote."
    }
    project.tasks.register<RepositoryTask>("repoPushAllTags") {
      doLast {
        for(repo in repositories.repositories.values) {
          if(!repo.update) continue
          println("Pushing all branches and annotated tags for repository $repo:")
          repo.pushAllTags(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push all local branches and annotated tags to the main remote."
    }

    // Shutdown JGit work queue after build is finished to free resources.
    project.gradle.buildFinished {
      WorkQueue.getExecutor().shutdown()
    }
  }
}

open class RepositoryTask : DefaultTask() {
  init {
    group = "repository"
  }
}
