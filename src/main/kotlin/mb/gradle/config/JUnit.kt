package mb.gradle.config

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.*
import java.io.PrintWriter
import java.io.StringWriter

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
  val extra = gradle.rootProject.extra
  val failedTestsProperty = "failedTests"
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
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
      }
      debug { // Show everything for debug logging level.
        events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
      }
    }
    afterTest(KotlinClosure2<TestDescriptor, TestResult, Unit>({ descriptor, result ->
      if(result.resultType == TestResult.ResultType.FAILURE) {
        if(!extra.has(failedTestsProperty)) {
          extra.set(failedTestsProperty, "")
        }
        val exception = result.exception
        val stacktrace = if(exception != null) {
          val stringWriter = StringWriter()
          val writer = PrintWriter(stringWriter)
          exception.printStackTrace(writer)
          writer.close()
          "\n$stringWriter"
        } else {
          ""
        }
        extra[failedTestsProperty] = "${extra[failedTestsProperty].toString()}\n${descriptor.className} > ${descriptor.name} FAILED$stacktrace"
        this.reports
      }
    }))
  }
  gradle.buildFinished {
    if(extra.has(failedTestsProperty)) {
      val failedTests = extra.get(failedTestsProperty)
      if(failedTests != null && !failedTests.toString().isBlank()) {
        println()
        println("> TEST FAILURE SUMMARY")
        println(failedTests)
        extra.set(failedTestsProperty, null)
      }
    }
  }
}
