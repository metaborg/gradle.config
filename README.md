[![GitHub license](https://img.shields.io/github/license/metaborg/gradle.config)](https://github.com/metaborg/gradle.config/blob/master/LICENSE)
[![GitHub build](https://img.shields.io/github/actions/workflow/status/metaborg/gradle.config/build.yml?label=GitHub%20build)](https://github.com/metaborg/gradle.config/actions/workflows/build.yml)
[![Jenkins build](https://img.shields.io/jenkins/build/https/buildfarm.metaborg.org/job/metaborg/job/gradle.config/job/master?label=Jenkins%20build)](https://buildfarm.metaborg.org/job/metaborg/job/gradle.config/job/master/lastBuild)
[![gradle.config](https://img.shields.io/maven-metadata/v?label=gradle.config&metadataUrl=https%3A%2F%2Fartifacts.metaborg.org%2Fcontent%2Frepositories%2Freleases%2Forg%2Fmetaborg%2Fgradle.config%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/org.metaborg/gradle.config?repo=metaborg-releases)


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


## Devenv Git Commands
The `org.metaborg.gradle.config.devenv` plugin allows managing Git sub-repositories (which can be submodules
or stand-alone repositories). The sub-repositories are defined in the `.properties` file associated with the
build script. For example:

```
myrepo=true
myrepo.update=true
myrepo.branch=develop
myrepo.submodule=true
```

For all sub-repositories, these commands are available: (Arguments: `--transport`, `--repo`)

- `list` - Lists the managed sub-repositories.
- `status` - Prints the status. (Argument: `--short`)
- `clone` - Clones those that have not been cloned yet.
- `fetch` - Fetches the latest changes.
- `checkout` - Checks out the correct branch.
- `update` - Updates the branch to the latest changes.
- `push` - Pushes the current local branch. (Arguments: `--all`, `--follow-tags`)
- `clean` - Removes untracked files and directories. (Arguments: `--force`, `--remove-ignored`)
- `reset` - Resets to the current commit. (Arguments: `--hard`)
- `commitSubmodules` - Creates a commit with the current commits of the submodules. (Arguments: `--message`)


## Applying
Apply the required plugins in the `build.gradle.kts` file's `plugins` block, like this:

    plugins {
      id("org.metaborg.gradle.config.java-library") version("0.5.6")
      id("org.metaborg.gradle.config.junit-testing") version("0.5.6")
    }

The latest version of the plugin can be found at the top of this readme.

## Development
This section details the development of this project.

### Building
This repository is built with Gradle, which requires a JDK of at least version 8 to be installed. Higher versions may work depending on [which version of Gradle is used](https://docs.gradle.org/current/userguide/compatibility.html).

To build this repository, run `./gradlew buildAll` on Linux and macOS, or `gradlew buildAll` on Windows.

### Automated Builds
All branches and tags of this repository are built on:
- [GitHub actions](https://github.com/metaborg/gradle.config/actions/workflows/build.yml) via `.github/workflows/build.yml`.
- Our [Jenkins buildfarm](https://buildfarm.metaborg.org/view/Devenv/job/metaborg/job/gradle.config/) via `Jenkinsfile` which uses our [Jenkins pipeline library](https://github.com/metaborg/jenkins.pipeline/).

### Usage
To use the developed plugin as part of the build of another project, include this plugin's build using the `--include-build` argument on the command line of Gradle. For example:

```shell
./gradlew build --include-build ./gradle.config
```

### Publishing
This repository is published via Gradle and Git with the [Gitonium](https://github.com/metaborg/gitonium) and [Gradle Config](https://github.com/metaborg/gradle.config) plugins.
It is published to our [artifact server](https://artifacts.metaborg.org) in the [releases repository](https://artifacts.metaborg.org/content/repositories/releases/).

First update `CHANGELOG.md` with your changes, create a new release entry, and update the release links at the bottom of the file.
Then, commit your changes.

To make a new release, create a tag in the form of `release-*` where `*` is the version of the release you'd like to make.
Then first build the project with `./gradlew buildAll` to check if building succeeds.

If you want our buildfarm to publish this release, just push the tag you just made, and our buildfarm will build the repository and publish the release.

If you want to publish this release locally, you will need an account with write access to our artifact server, and tell Gradle about this account.
Create the `./gradle/gradle.properties` file if it does not exist.
Add the following lines to it, replacing `<username>` and `<password>` with those of your artifact server account:
```
publish.repository.metaborg.artifacts.username=<username>
publish.repository.metaborg.artifacts.password=<password>
```
Then run `./gradlew publishAll` to publish all built artifacts.
You should also push the release tag you made such that this release is reproducible by others.

## Copyright and License
Copyright © 2018-2024 Delft University of Technology

The files in this repository are licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
You may use the files in this repository in compliance with the license.
