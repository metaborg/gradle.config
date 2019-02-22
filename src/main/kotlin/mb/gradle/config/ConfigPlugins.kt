package mb.gradle.config

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.plugins.dsl.KotlinDslPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

//
// Project plugins.
//

@Suppress("unused")
class RootProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureRootProject()
  }
}

@Suppress("unused")
class SubProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureSubProject()
  }
}

//
// Goal-based plugins.
//

@Suppress("unused")
class JavaLibraryPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJavaLibrary()
  }
}

@Suppress("unused")
class JavaApplicationPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJavaApplication()
  }
}

@Suppress("unused")
class JavaGradlePluginPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJavaGradlePlugin()
  }
}


@Suppress("unused")
class KotlinLibraryPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureKotlinLibrary()
  }
}

@Suppress("unused")
class KotlinApplicationPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureKotlinApplication()
  }
}

@Suppress("unused")
class KotlinGradlePluginPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureKotlinGradlePlugin()
  }
}


@Suppress("unused")
class JUnitTestingPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureJunitTesting()
  }
}

//
// Extension.
//

open class Config {
  var gradleWrapperVersion = "5.2.1"
  var javaVersion = JavaVersion.VERSION_1_8
  var kotlinApiVersion = "1.0"
  var kotlinLanguageVersion = "1.0"
  var junitVersion = "5.4.0"
}

@Suppress("unused")
open class MetaborgExtension(private val project: Project, val config: Config) {
  companion object {
    const val name = "metaborg"
  }


  fun configureSubProject() {
    project.configureSubProject()
  }


  fun configureJavaLibrary() {
    project.configureJavaLibrary()
  }

  fun configureJavaApplication() {
    project.configureJavaApplication()
  }


  fun configureKotlinLibrary() {
    project.configureKotlinLibrary()
  }

  fun configureKotlinApplication() {
    project.configureKotlinApplication()
  }


  fun configureJUnitTesting() {
    project.configureJunitTesting()
  }
}

//
// (Sub-)project configuration.
//

fun Project.configureRootProject() {
  configureVersion()
  configureAnyProject()
  createCompositeBuildTasks()
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureWrapper()
  }
}

fun Project.configureSubProject() {
  configureAnyProject()
  // Only root project needs version configuration, as the gitonium plugin handles sub-projects.
  // Only root project needs composite build tasks, as these tasks depend on tasks for sub-projects.
  // Only root project needs wrapper configuration.
}

private fun Project.configureAnyProject() {
  configureGroup()
  configureRepositories()
  configurePublishingRepositories()
  val config = Config() // Config is shared between root project and sub-projects.
  if(extensions.findByName(MetaborgExtension.name) == null) {
    extensions.add(MetaborgExtension.name, MetaborgExtension(this, config))
  }
  subprojects {
    if(extensions.findByName(MetaborgExtension.name) == null) {
      extensions.add(MetaborgExtension.name, MetaborgExtension(this, config))
    }
  }
}

//
// Shared configuration.
//

private fun Project.configureGroup() {
  group = "org.metaborg"
}

private fun Project.configureVersion() {
  pluginManager.apply("org.metaborg.gitonium")
}

private fun Project.configureRepositories() {
  val repoPrefix = "https://artifacts.metaborg.org/content/repositories"
  repositories {
    maven(url = "$repoPrefix/releases/")
    maven(url = "$repoPrefix/snapshots/")
    maven(url = "$repoPrefix/central/")
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
    if(project.subprojects.isEmpty()) {
      val task = project.tasks.findByName(name)
      if(task != null) {
        this.dependsOn(task)
      } else {
        project.logger.warn("Composite build task '$allName' does not include project '$project' because it does not have a task named '$name'")
      }
    } else {
      this.dependsOn(project.subprojects.mapNotNull {
        it.tasks.findByName(name) ?: run {
          project.logger.warn("Composite build task '$allName' does not include project '$it' because it does not have a task named '$name'")
          null
        }
      })
    }
  }
}

private fun Project.configureWrapper() {
  val config = extensions.getByType<MetaborgExtension>().config
  pluginManager.apply("wrapper")
  tasks {
    named<Wrapper>("wrapper") {
      gradleVersion = config.gradleWrapperVersion
      distributionType = Wrapper.DistributionType.ALL
      setJarFile(".gradlew/wrapper/gradle-wrapper.jar")
    }
  }
}

//
// Java configuration.
//

private fun Project.configureJavaVersion() {
  val config = extensions.getByType<MetaborgExtension>().config
  configure<JavaPluginExtension> {
    sourceCompatibility = config.javaVersion
    targetCompatibility = config.javaVersion
  }
}

private fun Project.configureJavaPublication(name: String, additionalConfiguration: MavenPublication.() -> Unit = {}) {
  pluginManager.apply("maven-publish")
  configure<PublishingExtension> {
    publications {
      create<MavenPublication>(name) {
        from(project.components["java"])
        additionalConfiguration()
      }
    }
  }
}

fun Project.configureJavaLibrary() {
  pluginManager.apply("java-library")
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJavaVersion()
    configureJavaPublication("JavaLibrary")
  }
}

private fun Project.configureJavaExecutableJar(publicationName: String) {
  // Create additional JAR task that creates an executable JAR.
  val jarTask = tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)
  val executableJarTask = tasks.register("executableJar", Jar::class) {
    manifest {
      attributes["Main-Class"] = project.the<JavaApplication>().mainClassName
    }
    archiveClassifier.set("executable")
    val runtimeClasspath by configurations
    from(runtimeClasspath.filter { it.exists() }.map { if(it.isDirectory) it else zipTree(it) })
    with(jarTask)
  }
  tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(executableJarTask)
  // Create an artifact for the executable JAR.
  val executableJarArtifact = artifacts.add("archives", executableJarTask) {
    classifier = "executable"
  }
  // Publish primary artifact from the Java component, and publish executable JAR and ZIP distribution as secondary artifacts.
  configureJavaPublication(publicationName) {
    artifact(executableJarArtifact)
    artifact(tasks.getByName("distZip"))
  }
}

fun Project.configureJavaApplication() {
  pluginManager.apply("application")
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJavaVersion()
    configureJavaExecutableJar("JavaApplication")
  }
}

fun Project.configureJavaGradlePlugin() {
  configureGradlePlugin()
}

//
// Kotlin configuration.
//

private fun Project.configureKotlinCompiler() {
  val config = extensions.getByType<MetaborgExtension>().config
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.apiVersion = config.kotlinApiVersion
    kotlinOptions.languageVersion = config.kotlinLanguageVersion
    kotlinOptions.jvmTarget = when(config.javaVersion) {
      JavaVersion.VERSION_1_6 -> "1.6"
      else -> "1.8"
    }
    kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility")
  }
}

private fun Project.configureKotlinStdlib() {
  val implementation by configurations
  dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
  }
}

fun Project.configureKotlinLibrary() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJavaVersion()
    configureKotlinCompiler()
    configureKotlinStdlib()
    configureJavaPublication("KotlinLibrary")
  }
}

fun Project.configureKotlinApplication() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  pluginManager.apply("application")
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJavaVersion()
    configureKotlinCompiler()
    configureKotlinStdlib()
    configureJavaExecutableJar("KotlinApplication")
  }
}

fun Project.configureKotlinGradlePlugin() {
  pluginManager.apply("org.jetbrains.kotlin.jvm")
  pluginManager.apply("org.gradle.kotlin.kotlin-dsl")
  configureGradlePlugin()
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJavaVersion()
    configureKotlinCompiler()
    // Do not configure Kotlin stdlib, since the 'kotlin-dsl' plugin already does this.
    val config = extensions.getByType<MetaborgExtension>().config
    extensions.configure<KotlinDslPluginOptions> {
      experimentalWarning.set(false)
      jvmTarget.set(when(config.javaVersion) {
        JavaVersion.VERSION_1_6 -> "1.6"
        else -> "1.8"
      })
    }
  }
}

//
// JUnit configuration.
//

private fun Project.configureJUnit() {
  val config = extensions.getByType<MetaborgExtension>().config
  val junitVersion = config.junitVersion
  val testImplementation by configurations
  val testRuntimeOnly by configurations
  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  }
  tasks.withType<Test> {
    useJUnitPlatform()
  }
}

fun Project.configureJunitTesting() {
  // Configure afterEvaluate, because it uses a property from an extension.
  afterEvaluate {
    configureJUnit()
  }
}

//
// Gradle plugin.
//

private fun Project.configureGradlePlugin() {
  pluginManager.apply("java-gradle-plugin")
  repositories {
    gradlePluginPortal() // Add plugin portal as a repository, to be able to depend on Gradle plugins.
  }
}
