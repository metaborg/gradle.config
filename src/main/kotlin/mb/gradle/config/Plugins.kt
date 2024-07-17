package mb.gradle.config

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.wrapper.Wrapper

@Suppress("unused")
open class MetaborgExtension(private val project: Project) {
    companion object {
        const val name = "metaborg"
    }


    var javaVersion = JavaVersion.VERSION_11
    var javaCreatePublication = true
    var javaCreateSourcesJar = true
    var javaPublishSourcesJar = true
    var javaCreateJavadocJar = false
    var javaPublishJavadocJar = false
    var kotlinApiVersion = "1.3"
    var kotlinLanguageVersion = "1.3"
    var junitVersion = "5.7.0"


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
