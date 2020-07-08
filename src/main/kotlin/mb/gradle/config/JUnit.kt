package mb.gradle.config

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.*

internal fun Project.configureJUnit() {
  val extension = extensions.getByType<MetaborgExtension>()
  val junitVersion = extension.junitVersion
  val testImplementation by configurations
  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
  }
  tasks.withType<Test> {
    @Suppress("UnstableApiUsage")
    useJUnitPlatform()
    testLogging {
      lifecycle {
        events(TestLogEvent.FAILED)
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
      }
      info {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
      }
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
