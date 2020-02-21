# Gradle Config Plugins
This project provides a number of plugins for a default consistent configuration
for the projects and a usable development environment. It includes the following
plugins:

For projects that use Java:

- `org.metaborg.gradle.config.java-library`  
  For projects that are Java libraries.
- `org.metaborg.gradle.config.java-application`  
  For projects that are Java applications.
- `org.metaborg.gradle.config.java-gradle-plugin`  
  For projects that use Java for writing Gradle plugins.

For projects that use Kotlin:

- `org.metaborg.gradle.config.kotlin-library`  
  For projects that are Kotlin libraries.
- `org.metaborg.gradle.config.kotlin-application`  
  For projects that are Kotlin applications.
- `org.metaborg.gradle.config.kotlin-testing-only`  
  For projects that use Kotlin for tests only.
- `org.metaborg.gradle.config.kotlin-gradle-plugin`  
  For projects that use Kotlin for writing Gradle plugins.

For projects that use JUnit:

- `org.metaborg.gradle.config.junit-testing`  
  For projects that use JUnit 5 for tests.

For projects in the project hierarchy:

- `org.metaborg.gradle.config.devenv`  
  For the root `/build.gradle.kts`, provides Git version control commands
- `org.metaborg.gradle.config.devenv-settings`  
  For the root `/settings.gradle.kts`
- `org.metaborg.gradle.config.root-project`  
  For a root composite build.
- `org.metaborg.gradle.config.sub-project`  
  Unused.


## Usage
Include the required plugins in the `build.gradle.kts` file's `plugins` block,
like this:

    plugins {
      id("org.metaborg.gradle.config.java-library")
      id("org.metaborg.gradle.config.junit-testing")
    }


## Development
To develop this plugin, first enable it in the root Gradle build.
In `/repo.properties`, ensure the following line is set to `true`:

    gradle.config=true

This will enable the project, ensuring that the developed version
is used instead of the released version whenever you build. It also
allows your IDE to recognize the project and its sources.


## Deployment
First, ensure your changes work correctly by [enabling the plugin](#development)
and building the project. Then push the changes to the `develop` branch.
Once the online build succeeds, merge the develop branch with `master`
and tag the commit to make a release. The tag format is: `release-1.2.3`.


## Git Version Control Tasks
The `org.metaborg.gradle.config.devenv` plugin, applied to the root of
this project, provides tasks for Git version control. The most important
tasks are:

- `repoStatus` — Provides the status of the repository.
- `repoUpdate` — Updates the repositories.
- `repoPush` — Pushes the current branch of the repository.

To list all tasks, issue:

    ./gradlew tasks

