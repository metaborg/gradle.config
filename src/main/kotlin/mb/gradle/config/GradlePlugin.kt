package mb.gradle.config

import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

internal fun Project.configureGradlePlugin() {
  pluginManager.apply("java-gradle-plugin")
  pluginManager.apply("maven-publish")
  repositories {
    @Suppress("UnstableApiUsage")
    gradlePluginPortal() // Add plugin portal as a repository, to be able to depend on Gradle plugins.
  }
}
