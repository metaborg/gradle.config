package mb.gradle.config.devenv

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

@Suppress("unused")
class DevenvSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val extension = DevenvSettingsExtension(settings)
        settings.extensions.add("devenv", extension)
    }
}

@Suppress("unused")
open class DevenvSettingsExtension(private val settings: Settings) {
    val repositoryConfigurations = RepositoryConfigurations.fromRootDirectory(settings.rootDir)

    fun includeBuildIfRepositoryIncluded(name: String) {
        if (repositoryConfigurations.isIncluded(name)) {
            includeBuildWithName(name, name)
        }
    }

    fun isRepositoryIncluded(name: String) = repositoryConfigurations.isIncluded(name)

    fun includeBuildWithName(directory: String, name: String) {
        settings.includeBuild(directory) {
            try {
                org.gradle.api.initialization.ConfigurableIncludedBuild::class.java
                    .getDeclaredMethod("setName", String::class.java)
                    .invoke(this, name)
            } catch (e: NoSuchMethodException) {
                // Running Gradle < 6, no need to set the name, ignore.
            }
        }
    }
}
