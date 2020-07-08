package mb.gradle.config

import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.*
import java.net.URI

fun Project.configureRootProject() {
  configureAnyProject()
  gradle.projectsEvaluated {
    // Create composite build tasks after all projects have been evaluated, to ensure that all tasks in all projects have been registered.
    createCompositeBuildTasks()
  }
}

fun Project.configureSubProject() {
  configureAnyProject()
  // Only root project needs composite build tasks, as these tasks depend on tasks for sub-projects.
}

fun Project.configureAnyProject() {
  configureGroup()
  configureRepositories()
  if(extensions.findByName(MetaborgExtension.name) == null) {
    extensions.add(MetaborgExtension.name, MetaborgExtension(this))
  }
  subprojects {
    if(extensions.findByName(MetaborgExtension.name) == null) {
      extensions.add(MetaborgExtension.name, MetaborgExtension(this))
    }
  }
  // Configure afterEvaluate, because...
  afterEvaluate {
    configureWrapper() // it uses a property from the metaborg extension, which may be modified by the user.
    configurePublishingRepositories() // it uses the version, which the Gitonium plugin may set later.
  }
}


private fun Project.configureGroup() {
  group = "org.metaborg"
}

private fun Project.configureRepositories() {
  repositories {
    maven(url = "https://artifacts.metaborg.org/content/groups/public/")
    mavenCentral() // Backup
  }
}

private fun Project.configurePublishingRepositories() {
  val isSnapshot = version.toString().contains("SNAPSHOT")
  val repoName = if(isSnapshot) "MetaborgSnapshotArtifacts" else "MetaborgReleaseArtifacts"
  val repoUrl = URI("https://artifacts.metaborg.org/content/repositories/${if(isSnapshot) "snapshots" else "releases"}/")
  val propPrefix = "publish.repository.metaborg.artifacts"
  pluginManager.apply("publishing")
  configure<PublishingExtension> {
    repositories {
      maven {
        name = repoName
        url = repoUrl
        credentials {
          username = project.findProperty("$propPrefix.username")?.toString()
          password = project.findProperty("$propPrefix.password")?.toString()
        }
      }
    }
  }
}

private fun Project.createCompositeBuildTasks() {
  tasks {
    createCompositeBuildTask(project, "cleanAll", "clean", "Deletes the build directory for all projects in the composite build.")
    createCompositeBuildTask(project, "checkAll", "check", "Runs all checks for all projects in the composite build.")
    createCompositeBuildTask(project, "assembleAll", "assemble", "Assembles the outputs for all projects in the composite build.")
    createCompositeBuildTask(project, "buildAll", "build", "Assembles and tests all projects in the composite build.")
    createCompositeBuildTask(project, "publishAll", "publish", "Publishes all publications produced by all projects in the composite build.")
  }
}

private fun TaskContainerScope.createCompositeBuildTask(project: Project, allName: String, name: String, description: String) {
  register(allName) {
    this.group = "composite build"
    this.description = description
    if(project.subprojects.isEmpty() && project.gradle.includedBuilds.isEmpty()) {
      // Root project without sub-projects nor included builds: just depend on task in the current project.
      val task = project.tasks.findByName(name)
      if(task != null) {
        this.dependsOn(task)
      } else {
        project.logger.warn("Composite build task '$allName' does not delegate to $project because it does not have a task named '$name'")
      }
    } else {
      // Root project with sub-projects: depend on tasks of sub-projects and included builds.
      this.dependsOn(project.subprojects.mapNotNull {
        it.tasks.findByName(name) ?: run {
          project.logger.warn("Composite build task '$allName' does not delegate to $it because it does not have a task named '$name'")
          null
        }
      })
      this.dependsOn(project.gradle.includedBuilds.mapNotNull {
        try {
          it.task(":$allName")
        } catch(e: Throwable) {
          project.logger.warn("Composite build task '$allName' does not include $it because it does not have a task named '$allName'")
        }
      })
    }
  }
}

private fun Project.configureWrapper() {
  val extension = extensions.getByType<MetaborgExtension>()
  fun Wrapper.configureWrapperTask() {
    gradleVersion = extension.gradleWrapperVersion
    distributionType = extension.gradleWrapperDistribution
    setJarFile(".gradlew/wrapper/gradle-wrapper.jar")
  }
  try {
    tasks.named<Wrapper>("wrapper") {
      configureWrapperTask()
    }
  } catch(e: UnknownTaskException) {
    // Create wrapper task if it does not exist (which seems to be the case for sub projects of root projects)
    tasks.register("wrapper", Wrapper::class.java) {
      configureWrapperTask()
    }
  }
}
