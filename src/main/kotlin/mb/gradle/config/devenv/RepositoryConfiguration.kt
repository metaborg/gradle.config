package mb.gradle.config.devenv

import org.gradle.api.GradleException
import java.io.File
import java.util.*

/** The configuration of a Git repository. */
data class RepositoryConfiguration(
  /** The name of the repository, for debugging purposes. */
  val name: String,
  /** Whether the repository is included in the build. */
  val include: Boolean,
  /** Whether to modify the repository as part of this plugin's tasks. */
  val update: Boolean,
  /** The directory of the repository, relative to the root directory. */
  val directory: String,
  /** The URL of the repository to clone from. */
  val url: String? = null,
  /** The branch of the repository to use; or `null` to use the root repository's branch. */
  val branch: String? = null,
  /** The remote name, say `"origin"`; or `null` to use the repository's default (`checkout.defaultRemote`). */
  val remote: String? = null,
  /** Whether the repository is a submodule. */
  val submodule: Boolean = false
)

data class RepositoryConfigurations(
  val rootDirectory: File,
  val urlPrefix: String,
  val configurations: Map<String, RepositoryConfiguration>
) {
  fun isIncluded(name: String): Boolean {
    val config = configurations[name] ?: return false
    return config.include && rootDirectory.resolve(config.directory).exists()
  }

  fun isUpdated(name: String): Boolean {
    val config = configurations[name] ?: return false
    return config.update && rootDirectory.resolve(config.directory).exists()
  }


  companion object {
    fun fromRootDirectory(rootDirectory: File): RepositoryConfigurations {
      return fromPropertiesFile(rootDirectory, rootDirectory.resolve("repo.properties"))
    }

    private fun fromPropertiesFile(rootDirectory: File, propertiesFile: File): RepositoryConfigurations {
      if(!propertiesFile.isFile) {
        throw GradleException("Cannot read repository configuration from properties file, '$propertiesFile' does not exist or is not a file")
      }
      val properties = Properties()
      propertiesFile.inputStream().buffered().use { inputStream ->
        properties.load(inputStream)
      }
      return fromProperties(rootDirectory, properties)
    }

    private fun fromProperties(rootDirectory: File, properties: Properties): RepositoryConfigurations {
      data class Configuration(
        val name: String,
        var include: Boolean? = null,
        var update: Boolean? = null,
        var directory: String? = null,
        var url: String? = null,
        var branch: String? = null,
        var remote: String? = null,
        var submodule: Boolean? = null
      ) {
        fun toImmutable() = RepositoryConfiguration(
          name,
          include ?: false,
          update ?: include ?: false,
          directory ?: name,
          url,
          branch,
          remote,
          submodule ?: false
        )
      }

      var urlPrefix = "git@github.com:metaborg"
      val configurations = HashMap<String, Configuration>()
      for((k, v) in properties.entries) {
        k as String
        v as String
        when {
          k == "urlPrefix" -> {
            urlPrefix = v
          }
          k.endsWith(".update") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.update = v == "true"
          }
          k.endsWith(".dir") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.directory = v
          }
          k.endsWith(".url") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.url = v
          }
          k.endsWith(".branch") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.branch = v
          }
          k.endsWith(".remote") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.remote = v
          }
          k.endsWith(".submodule") -> {
            val name = k.substringBeforeLast('.')
            val config = configurations.getOrPut(name) { Configuration(name) }
            config.submodule = v == "true"
          }
          k.endsWith(".jenkinsjob") -> {
            // Ignore
          }
          else -> {
            val config = configurations.getOrPut(k) { Configuration(k) }
            config.include = v == "true"
          }
        }
      }
      return RepositoryConfigurations(rootDirectory, urlPrefix, configurations.mapValues { it.value.toImmutable() })
    }
  }
}
