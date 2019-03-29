package mb.gradle.config.devenv

import org.gradle.api.DefaultTask

open class DevenvTask : DefaultTask() {
  init {
    group = "devenv"
  }
}
