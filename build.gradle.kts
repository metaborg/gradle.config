plugins {
    id("org.metaborg.gradle.config.root-project") version "0.7.3"         // Bootstrap
    id("org.metaborg.gradle.config.kotlin-gradle-plugin") version "0.7.3" // Bootstrap
    id("org.metaborg.gitonium") version "1.2.0"
    kotlin("jvm") version "1.7.10" // Stick with 1.7.10: Gradle 7.6.4's kotlin-dsl plugin uses it.
    `kotlin-dsl`
    `maven-publish`
}

metaborg {
    kotlinApiVersion = "1.4"
    kotlinLanguageVersion = "1.4"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")

    // Compile-only dependencies for Gradle plugins that we need to use types from, but should still be applied/provided by users.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    compileOnly("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.4.0")

    testImplementation("io.kotest:kotest-runner-junit5:4.2.0")
    testImplementation("io.kotest:kotest-assertions-core:4.2.0")
    testImplementation(gradleTestKit())
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
        create("metaborg-devenv-repositories") {
            id = "org.metaborg.gradle.config.devenv-repositories"
            implementationClass = "mb.gradle.config.devenv.DevenvRepositoriesPlugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
