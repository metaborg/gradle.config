[![GitHub license](https://img.shields.io/github/license/metaborg/gradle.config)](https://github.com/metaborg/gradle.config/blob/master/LICENSE)
[![Jenkins](https://img.shields.io/jenkins/build/https/buildfarm.metaborg.org/job/metaborg/job/gradle.config/job/master)](https://buildfarm.metaborg.org/job/metaborg/job/gradle.config/job/master/lastBuild)
[![coronium](https://img.shields.io/maven-metadata/v?label=gradle.config&metadataUrl=https%3A%2F%2Fartifacts.metaborg.org%2Fcontent%2Frepositories%2Freleases%2Forg%2Fmetaborg%2Fgradle.config%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/org.metaborg/gradle.config?repo=metaborg-releases)


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


## Deployment
First, ensure your changes work correctly by building the project.
Then push the changes to the `develop` branch.
Once the online build succeeds, merge the develop branch into `master` and tag the commit to make a release.
The tag format is: `release-1.2.3`.
