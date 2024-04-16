package mb.gradle.config

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType

fun Project.configureJunitTesting() {
    // Configure afterEvaluate, because...
    afterEvaluate {
        // these use a property from the metaborg extension, which may be modified by the user.
        configureJUnit()
    }
}


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
            lifecycle { // Only show test failures for lifecycle (default) logging level.
                events(TestLogEvent.FAILED)
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
            info { // Show failed and skipped tests, and standard output and error, for info logging level.
                events(
                    TestLogEvent.FAILED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                    TestLogEvent.STANDARD_ERROR
                )
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
            debug { // Show everything for debug logging level.
                events(
                    TestLogEvent.STARTED,
                    TestLogEvent.PASSED,
                    TestLogEvent.FAILED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                    TestLogEvent.STANDARD_ERROR
                )
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}
