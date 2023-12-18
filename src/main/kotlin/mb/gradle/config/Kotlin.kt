package mb.gradle.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.plugins.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.configureKotlinLibrary() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  // Configure stdlib eagerly: Kotlin plugin will modify the version to its applied plugin version in afterEvaluate.
  configureKotlinStdlib(configurations.getByName("implementation"))
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureKotlinCompiler()
    configureJavaPublication("KotlinLibrary")
  }
}

fun Project.configureKotlinApplication() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  pluginManager.apply("application")
  // Configure stdlib eagerly: Kotlin plugin will modify the version to its applied plugin version in afterEvaluate.
  configureKotlinStdlib(configurations.getByName("implementation"))
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureKotlinCompiler()
    configureJavaExecutableJar("KotlinApplication")
  }
}

fun Project.configureKotlinTestingOnly() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  // Configure stdlib eagerly: Kotlin plugin will modify the version to its applied plugin version in afterEvaluate.
  configureKotlinStdlib(configurations.getByName("testImplementation"))
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureKotlinCompiler()
  }
}

fun Project.configureKotlinGradlePlugin() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  pluginManager.apply("org.gradle.kotlin.kotlin-dsl") // Do not configure Kotlin stdlib, since the 'kotlin-dsl' plugin already does this.
  configureGradlePlugin()
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureKotlinCompiler()
    val extension = extensions.getByType<MetaborgExtension>()
    extensions.configure<KotlinDslPluginOptions> {
      jvmTarget.set(when(extension.javaVersion) {
        JavaVersion.VERSION_1_6 -> "1.6"
        JavaVersion.VERSION_1_8 -> "1.8"
        else -> extension.javaVersion.majorVersion
      })
    }
  }
}


private fun Project.configureKotlinCompiler() {
  val extension = extensions.getByType<MetaborgExtension>()
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.apiVersion = extension.kotlinApiVersion
    kotlinOptions.languageVersion = extension.kotlinLanguageVersion
    kotlinOptions.jvmTarget = when(extension.javaVersion) {
      JavaVersion.VERSION_1_6 -> "1.6"
      JavaVersion.VERSION_1_8 -> "1.8"
      else -> extension.javaVersion.majorVersion
    }
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility")
  }
}

private fun Project.configureKotlinStdlib(configuration: Configuration) {
  dependencies {
    configuration(kotlin("stdlib"))
    configuration(kotlin("stdlib-jdk8"))
  }
}
