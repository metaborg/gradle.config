package mb.gradle.config.devenv

import org.gradle.api.GradleException
import java.io.File
import java.util.*

data class RepositoryConfiguration(
  val name: String,
  val include: Boolean,
  val update: Boolean,
  val directory: String,
  val url: String? = null,
  val branch: String? = null
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
        var dirPath: String? = null,
        var url: String? = null,
        var branch: String? = null
      ) {
        fun toImmutable() = RepositoryConfiguration(name, include ?: false, update ?: include ?: false, dirPath
          ?: name, url, branch)
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
            config.dirPath = v
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
