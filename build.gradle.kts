plugins {
  id("org.metaborg.gradle.config.root-project") version "0.3.0" // Bootstrap with previous version.
  id("org.metaborg.gitonium") version "0.1.0"
  kotlin("jvm") version "1.3.20"
  `kotlin-dsl`
  `java-gradle-plugin`
  `maven-publish`
}

repositories {
  gradlePluginPortal() // Add plugin portal as a repository, to be able to depend on Gradle plugins.
}

dependencies {
  compile("org.eclipse.jgit:org.eclipse.jgit:5.2.0.201812061821-r")
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.20")
  compileOnly("org.gradle.kotlin:plugins:1.2.2")
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
    create("metaborg-java-gradle-plugin") {
      id = "org.metaborg.gradle.config.java-gradle-plugin"
      implementationClass = "mb.gradle.config.JavaGradlePluginPlugin"
    }

    create("metaborg-kotlin-library") {
      id = "org.metaborg.gradle.config.kotlin-library"
      implementationClass = "mb.gradle.config.KotlinLibraryPlugin"
    }
    create("metaborg-kotlin-application") {
      id = "org.metaborg.gradle.config.kotlin-application"
      implementationClass = "mb.gradle.config.KotlinApplicationPlugin"
    }
    create("metaborg-kotlin-gradle-plugin") {
      id = "org.metaborg.gradle.config.kotlin-gradle-plugin"
      implementationClass = "mb.gradle.config.KotlinGradlePluginPlugin"
    }

    create("metaborg-junit-testing") {
      id = "org.metaborg.gradle.config.junit-testing"
      implementationClass = "mb.gradle.config.JUnitTestingPlugin"
    }

    create("metaborg-devenv") {
      id = "org.metaborg.gradle.config.devenv"
      implementationClass = "mb.gradle.config.DevenvPlugin"
    }
  }
}
