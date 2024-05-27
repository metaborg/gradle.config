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

    fun isRepositoryIncluded(name: String) = repositoryConfigurations.isIncluded(name)

}
