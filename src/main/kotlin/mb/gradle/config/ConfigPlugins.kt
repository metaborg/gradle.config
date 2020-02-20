package mb.gradle.config

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.plugins.dsl.*
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
class KotlinTestingOnlyPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.configureKotlinTestingOnly()
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

@Suppress("unused")
open class MetaborgExtension(private val project: Project) {
  companion object {
    const val name = "metaborg"
  }


  var gradleWrapperVersion = "5.6.4"
  var gradleWrapperDistribution = Wrapper.DistributionType.BIN
  var javaVersion = JavaVersion.VERSION_1_8
  var javaCreatePublication = true
  var javaCreateSourcesJar = true
  var javaPublishSourcesJar = true
  var javaCreateJavadocJar = false
  var javaPublishJavadocJar = false
  var kotlinApiVersion = "1.0"
  var kotlinLanguageVersion = "1.0"
  var junitVersion = "5.6.0"


  fun configureSubProject() {
    project.configureSubProject()
  }


  fun configureJavaLibrary() {
    project.configureJavaLibrary()
  }

  fun configureJavaApplication() {
    project.configureJavaApplication()
  }

  fun configureJavaGradlePlugin() {
    project.configureJavaGradlePlugin()
  }


  fun configureKotlinLibrary() {
    project.configureKotlinLibrary()
  }

  fun configureKotlinApplication() {
    project.configureKotlinApplication()
  }

  fun configureKotlinTestingOnly() {
    project.configureKotlinTestingOnly()
  }

  fun configureKotlinGradlePlugin() {
    project.configureKotlinGradlePlugin()
  }


  fun configureJUnitTesting() {
    project.configureJunitTesting()
  }
}

//
// (Sub-)project configuration.
//

fun Project.configureRootProject() {
  configureAnyProject()
  gradle.projectsEvaluated {
    // Create composite build tasks after all projects have been evaluated, to ensure that all tasks in all projects have been registered.
    createCompositeBuildTasks()
  }
}

fun Project.configureSubProject() {
  configureAnyProject()
  // Only root project needs composite build tasks, as these tasks depend on tasks for sub-projects.
}

fun Project.configureAnyProject() {
  configureGroup()
  configureRepositories()
  if(extensions.findByName(MetaborgExtension.name) == null) {
    extensions.add(MetaborgExtension.name, MetaborgExtension(this))
  }
  subprojects {
    if(extensions.findByName(MetaborgExtension.name) == null) {
      extensions.add(MetaborgExtension.name, MetaborgExtension(this))
    }
  }
  // Configure afterEvaluate, because...
  afterEvaluate {
    configureWrapper() // it uses a property from the metaborg extension, which may be modified by the user.
    configurePublishingRepositories() // it uses the version, which the Gitonium plugin may set later.
  }
}

//
// Shared configuration.
//

private fun Project.configureGroup() {
  group = "org.metaborg"
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
    if(project.subprojects.isEmpty() && project.gradle.includedBuilds.isEmpty()) {
      // Root project without sub-projects nor included builds: just depend on task in the current project.
      val task = project.tasks.findByName(name)
      if(task != null) {
        this.dependsOn(task)
      } else {
        project.logger.warn("Composite build task '$allName' does not delegate to $project because it does not have a task named '$name'")
      }
    } else {
      // Root project with sub-projects: depend on tasks of sub-projects and included builds.
      this.dependsOn(project.subprojects.mapNotNull {
        it.tasks.findByName(name) ?: run {
          project.logger.warn("Composite build task '$allName' does not delegate to $it because it does not have a task named '$name'")
          null
        }
      })
      this.dependsOn(project.gradle.includedBuilds.mapNotNull {
        try {
          it.task(":$allName")
        } catch(e: Throwable) {
          project.logger.warn("Composite build task '$allName' does not include $it because it does not have a task named '$allName'")
        }
      })
    }
  }
}

private fun Project.configureWrapper() {
  val extension = extensions.getByType<MetaborgExtension>()
  fun Wrapper.configureWrapperTask() {
    gradleVersion = extension.gradleWrapperVersion
    distributionType = extension.gradleWrapperDistribution
    setJarFile(".gradlew/wrapper/gradle-wrapper.jar")
  }
  try {
    tasks.named<Wrapper>("wrapper") {
      configureWrapperTask()
    }
  } catch(e: UnknownTaskException) {
    // Create wrapper task if it does not exist (which seems to be the case for sub projects of root projects)
    tasks.register("wrapper", Wrapper::class.java) {
      configureWrapperTask()
    }
  }
}

//
// Java configuration.
//

private fun Project.configureJavaCompiler() {
  val extension = extensions.getByType<MetaborgExtension>()
  @Suppress("UnstableApiUsage")
  configure<JavaPluginExtension> {
    sourceCompatibility = extension.javaVersion
    targetCompatibility = extension.javaVersion
  }
  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
  }
}

private fun Project.configureJavaSourcesJar() {
  val extension = extensions.getByType<MetaborgExtension>()
  if(!extension.javaCreateSourcesJar) {
    return
  }

  val sourceSets = extensions.getByType<SourceSetContainer>()
  val sourcesJarTask = tasks.create<Jar>("sourcesJar") {
    dependsOn(tasks.getByName(JavaPlugin.CLASSES_TASK_NAME))
    from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).allJava)
    archiveClassifier.set("sources")
  }
  tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(sourcesJarTask)
  artifacts.add(Dependency.DEFAULT_CONFIGURATION, sourcesJarTask) {
    classifier = "sources"
  }
}

private fun Project.configureJavadocJar() {
  tasks {
    named<Javadoc>(JavaPlugin.JAVADOC_TASK_NAME) {
      isFailOnError = false

      val optionsFile = temporaryDir.resolve("javadoc_options.txt")
      optionsFile.writeText("-Xdoclint:none\n")
      options {
        optionFiles(optionsFile)
      }
    }
  }

  val extension = extensions.getByType<MetaborgExtension>()
  if(!extension.javaCreateJavadocJar) {
    return
  }

  val javadocJarTask = tasks.create<Jar>("javadocJar") {
    val javadocTask = tasks.getByName(JavaPlugin.JAVADOC_TASK_NAME)
    dependsOn(javadocTask)
    from(javadocTask)
    archiveClassifier.set("javadoc")
  }
  tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(javadocJarTask)
  artifacts.add(Dependency.DEFAULT_CONFIGURATION, javadocJarTask) {
    classifier = "javadoc"
  }
}

private fun Project.configureJavaPublication(name: String, additionalConfiguration: MavenPublication.() -> Unit = {}) {
  val extension = extensions.getByType<MetaborgExtension>()
  if(!extension.javaCreatePublication) {
    return
  }

  pluginManager.apply("maven-publish")
  configure<PublishingExtension> {
    publications {
      create<MavenPublication>(name) {
        // Add primary artifact.
        from(project.components["java"])
        // Add sources JAR artifact.
        if(extension.javaPublishSourcesJar) {
          val sourcesJarTask = tasks.findByName("sourcesJar")
          if(sourcesJarTask != null) {
            artifact(sourcesJarTask) {
              classifier = "sources"
            }
          }
        }
        // Add javadoc JAR artifact.
        if(extension.javaPublishJavadocJar) {
          val javadocJarTask = tasks.findByName("javadocJar")
          if(javadocJarTask != null) {
            artifact(javadocJarTask) {
              classifier = "javadoc"
            }
          }
        }
        // Run any additional configuration
        additionalConfiguration()
      }
    }
  }
}

fun Project.configureJavaLibrary() {
  pluginManager.apply("java-library")
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureJavaSourcesJar()
    configureJavadocJar()
    configureJavaPublication("JavaLibrary")
  }
}

private fun Project.configureJavaExecutableJar(publicationName: String) {
  // Create additional JAR task that creates an executable JAR.
  val jarTask = tasks.getByName<Jar>(JavaPlugin.JAR_TASK_NAME)
  val executableJarTask = tasks.register<Jar>("executableJar") {
    val runtimeClasspath by configurations
    dependsOn(runtimeClasspath)

    archiveClassifier.set("executable")

    with(jarTask)
    from({
      // Closure inside to defer evaluation until task execution time.
      runtimeClasspath.filter { it.exists() }.map {
        @Suppress("IMPLICIT_CAST_TO_ANY") // Implicit cast to Any is fine, as from takes Any's.
        if(it.isDirectory) it else zipTree(it)
      }
    })

    doFirst { // Delay setting Main-Class attribute to just before execution, to ensure that mainClassName is set.
      manifest {
        @Suppress("UnstableApiUsage")
        attributes["Main-Class"] = project.the<JavaApplication>().mainClassName
      }
    }
  }
  tasks.named(BasePlugin.ASSEMBLE_TASK_NAME).configure { dependsOn(executableJarTask) }
  // Create an artifact for the executable JAR.
  val executableJarArtifact = artifacts.add(Dependency.DEFAULT_CONFIGURATION, executableJarTask) {
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
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJavaCompiler()
    configureJavaSourcesJar()
    configureJavadocJar()
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
  val extension = extensions.getByType<MetaborgExtension>()
  tasks.withType<KotlinCompile>().all {
    kotlinOptions.apiVersion = extension.kotlinApiVersion
    kotlinOptions.languageVersion = extension.kotlinLanguageVersion
    kotlinOptions.jvmTarget = when(extension.javaVersion) {
      JavaVersion.VERSION_1_6 -> "1.6"
      else -> "1.8"
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
      experimentalWarning.set(false)
      jvmTarget.set(when(extension.javaVersion) {
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
  val extension = extensions.getByType<MetaborgExtension>()
  val junitVersion = extension.junitVersion
  val testImplementation by configurations
  dependencies {
    // NOTE: This is an aggregate dependency that includes:
    //   testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    //   testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    //   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
  }
  tasks.withType<Test> {
    @Suppress("UnstableApiUsage")
    useJUnitPlatform()
    testLogging {
      events(TestLogEvent.FAILED)
      showExceptions = true
      showCauses = true
      showStackTraces = true
      exceptionFormat = TestExceptionFormat.FULL
    }
  }
}

fun Project.configureJunitTesting() {
  // Configure afterEvaluate, because...
  afterEvaluate {
    // these use a property from the metaborg extension, which may be modified by the user.
    configureJUnit()
  }
}

//
// Gradle plugin.
//

private fun Project.configureGradlePlugin() {
  pluginManager.apply("java-gradle-plugin")
  pluginManager.apply("maven-publish")
  repositories {
    @Suppress("UnstableApiUsage")
    gradlePluginPortal() // Add plugin portal as a repository, to be able to depend on Gradle plugins.
  }
}
