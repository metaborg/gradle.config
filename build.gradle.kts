plugins {
  id("org.metaborg.gradle.config.root-project") version "0.3.1" // Bootstrap with previous version.
  id("org.metaborg.gradle.config.kotlin-gradle-plugin") version "0.3.1" // Bootstrap with previous version.
  id("org.metaborg.gitonium") version "0.1.2"
  kotlin("jvm") version "1.3.21"
  `kotlin-dsl`
  `maven-publish`
}

metaborg {
  config.kotlinApiVersion = "1.2"
  config.kotlinLanguageVersion = "1.2"
}

dependencies {
  implementation("org.eclipse.jgit:org.eclipse.jgit:5.3.1.201904271842-r")
  
  // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
  compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.20")
  compileOnly("org.gradle.kotlin:plugins:1.2.2")
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
    create("metaborg-kotlin-testing-only") {
      id = "org.metaborg.gradle.config.kotlin-testing-only"
      implementationClass = "mb.gradle.config.KotlinTestingOnlyPlugin"
    }
    create("metaborg-kotlin-gradle-plugin") {
      id = "org.metaborg.gradle.config.kotlin-gradle-plugin"
      implementationClass = "mb.gradle.config.KotlinGradlePluginPlugin"
    }

    create("metaborg-junit-testing") {
      id = "org.metaborg.gradle.config.junit-testing"
      implementationClass = "mb.gradle.config.JUnitTestingPlugin"
    }

    create("metaborg-devenv-settings") {
      id = "org.metaborg.gradle.config.devenv-settings"
      implementationClass = "mb.gradle.config.devenv.DevenvSettingsPlugin"
    }
    create("metaborg-devenv") {
      id = "org.metaborg.gradle.config.devenv"
      implementationClass = "mb.gradle.config.devenv.DevenvPlugin"
    }
  }
}
