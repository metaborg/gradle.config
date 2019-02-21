plugins {
  //id("org.metaborg.gradle.config.root-project") version "0.5.0" // Bootstrap with previous version.
  //id("org.metaborg.gitonium") version "0.3.0"
  kotlin("jvm") version "1.3.20"
  `kotlin-dsl`
  `java-gradle-plugin`
  publishing
  `maven-publish`
}

group = "org.metaborg"
version = "0.1.0"

dependencies {
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.20")
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}
gradlePlugin {
  plugins {
    create("metaborg-root-project") {
      id = "org.metaborg.gradle.config.root-project"
      implementationClass = "mb.gradle.config.RootProjectPlugin"
    }
    create("metaborg-sub-project") {
      id = "org.metaborg.gradle.config.sub-project"
      implementationClass = "mb.gradle.config.SubProjectPlugin"
    }

    create("metaborg-java-library") {
      id = "org.metaborg.gradle.config.java-library"
      implementationClass = "mb.gradle.config.JavaLibraryPlugin"
    }
    create("metaborg-java-application") {
      id = "org.metaborg.gradle.config.java-application"
      implementationClass = "mb.gradle.config.JavaApplicationPlugin"
    }
    create("metaborg-kotlin-library") {
      id = "org.metaborg.gradle.config.kotlin-library"
      implementationClass = "mb.gradle.config.KotlinLibraryPlugin"
    }

    create("metaborg-devenv") {
      id = "org.metaborg.gradle.config.devenv"
      implementationClass = "mb.gradle.config.DevenvPlugin"
    }
  }
}

configure<PublishingExtension> {
  repositories {
    maven {
      name = "MetaborgArtifacts"
      url = if(version.toString().contains("SNAPSHOT")) {
        uri("https://artifacts.metaborg.org/content/repositories/snapshots/")
      } else {
        uri("https://artifacts.metaborg.org/content/repositories/releases/")
      }
      credentials {
        username = project.findProperty("publish.repository.metaborg.artifacts.username")?.toString()
        password = project.findProperty("publish.repository.metaborg.artifacts.password")?.toString()
      }
    }
  }
}

tasks {
  wrapper {
    gradleVersion = "5.2.1"
    distributionType = Wrapper.DistributionType.ALL
    setJarFile(".gradlew/wrapper/gradle-wrapper.jar")
  }
}

repositories {
  maven(url = "https://artifacts.metaborg.org/content/repositories/releases/")
  maven(url = "https://artifacts.metaborg.org/content/repositories/snapshots/")
  maven(url = "https://artifacts.metaborg.org/content/repositories/central/")
  mavenCentral() // Backup
}
