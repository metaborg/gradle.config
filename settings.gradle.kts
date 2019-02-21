rootProject.name = "gradle.config"

pluginManagement {
  repositories {
    maven(url = "https://artifacts.metaborg.org/content/repositories/releases/")
    maven(url = "https://artifacts.metaborg.org/content/repositories/snapshots/")
    gradlePluginPortal()
  }
}
