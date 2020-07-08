package mb.gradle.config

import org.gradle.api.Project
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
import org.gradle.kotlin.dsl.*

internal fun Project.configureJavaCompiler() {
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

internal fun Project.configureJavaSourcesJar() {
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

internal fun Project.configureJavadocJar() {
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

internal fun Project.configureJavaPublication(name: String, additionalConfiguration: MavenPublication.() -> Unit = {}) {
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

internal fun Project.configureJavaExecutableJar(publicationName: String) {
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
