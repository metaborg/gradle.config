package mb.gradle.config.devenv

import org.eclipse.jgit.lib.internal.WorkQueue
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class DevenvPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = DevenvExtension(project)
    project.extensions.add("devenv", extension)
    project.afterEvaluate {
      configure(this, extension)
    }
  }

  private fun configure(project: Project, extension: DevenvExtension) {
    val urlPrefix = extension.repoUrlPrefix
      ?: throw GradleException("Cannot create repositories of devenv; repository URL prefix (repoUrlPrefix) was not set in 'devenv' extension")
    val properties = repoProperties(project.rootDir)
    val repoProperties = toRepoConfigMap(properties)
    val rootBranch = branch(project.rootDir)
    val repos = createRepos(extension.repoConfigs, repoProperties, urlPrefix, rootBranch)

    project.tasks.register<DevenvTask>("repoList") {
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
    project.tasks.register<DevenvTask>("repoStatus") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Status for repository ${repo.dirPath}:")
          repo.status(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: show its status"
    }
    project.tasks.register<DevenvTask>("repoClone") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          if(repo.isCheckedOut(project.rootDir)) continue
          println("Cloning repository ${repo.dirPath}:")
          repo.clone(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: clone the repository if it has not been cloned yet."
    }
    project.tasks.register<DevenvTask>("repoFetch") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Fetching for repository ${repo.dirPath}:")
          repo.fetch(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: fetch from the main remote."
    }
    project.tasks.register<DevenvTask>("repoCheckout") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Checking out ${repo.branch} for repository ${repo.dirPath}:")
          repo.checkout(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: checkout the correct branch."
    }
    project.tasks.register<DevenvTask>("repoUpdate") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          if(!repo.isCheckedOut(project.rootDir)) {
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
    project.tasks.register<DevenvTask>("repoPush") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Pushing current branch for repository ${repo.dirPath}:")
          repo.push(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push the current local branch to the main remote."
    }
    project.tasks.register<DevenvTask>("repoPushTags") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Pushing current branch and annotated tags for repository ${repo.dirPath}:")
          repo.pushTags(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push the current local branch and annotated tags to the main remote."
    }
    project.tasks.register<DevenvTask>("repoPushAll") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Pushing all branches for repository ${repo.dirPath}:")
          repo.pushAll(project)
          println()
        }
      }
      description = "For each Git repository of devenv for which update is set to true: push all local branches to the main remote."
    }
    project.tasks.register<DevenvTask>("repoPushAllTags") {
      doLast {
        for(repo in repos) {
          if(!repo.update) continue
          println("Pushing all branches and annotated tags for repository ${repo.dirPath}:")
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

@Suppress("unused")
open class DevenvExtension(private val project: Project) {
  var repoUrlPrefix: String? = null

  @JvmOverloads
  fun registerRepo(name: String, defaultInclude: Boolean? = null, defaultUpdate: Boolean? = null, defaultUrl: String? = null, defaultBranch: String? = null, defaultDirPath: String? = null) {
    repoConfigs[name] = RepoConfig(defaultInclude, defaultUpdate, defaultDirPath, defaultUrl, defaultBranch)
  }

  internal val repoConfigs = HashMap<String, RepoConfig>()
}
