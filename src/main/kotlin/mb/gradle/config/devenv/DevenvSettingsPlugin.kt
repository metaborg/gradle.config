package mb.gradle.config.devenv

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

@Suppress("unused")
class DevenvSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    val extension = DevenvSettingsExtension(settings)
    settings.extensions.add("devenv", extension)
  }
}

@Suppress("unused")
open class DevenvSettingsExtension(private val settings: Settings) {
  val repoProperties = repoProperties(properties(settings.rootDir))

  fun includeBuildsFromSubDirs(onlyIncludeBuildsFromIncludedRepos: Boolean) {
    val rootDir = settings.rootDir

    val includedRepoDirPaths = if(onlyIncludeBuildsFromIncludedRepos) {
      val includedRepoDirPaths = HashSet<String>()
      for((name, repoProperty) in repoProperties.entries) {
        if(repoProperty.include == true) {
          val dirPath = repoProperty.dirPath ?: name
          includedRepoDirPaths.add(dirPath)
        }
      }
      includedRepoDirPaths
    } else {
      HashSet()
    }

    rootDir.list().forEach { dirPath ->
      if(File(rootDir, "$dirPath/${Settings.DEFAULT_SETTINGS_FILE}").isFile || File(rootDir, "$dirPath/${Settings.DEFAULT_SETTINGS_FILE}.kts").isFile) {
        if(!onlyIncludeBuildsFromIncludedRepos || includedRepoDirPaths.contains(dirPath)) {
          settings.includeBuild(dirPath)
        }
      }
    }
  }
}
