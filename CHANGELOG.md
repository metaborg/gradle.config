# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]


## [0.5.7] - 2024-04-16
- No changes.


## [0.5.6] - 2024-04-16
- Allow duplicate resources when building Jars.


## [0.5.5] - 2023-10-06
- Fix `./repo update` not always updating sub repositories.
- Add `./repo commitSubmodules` command to commit latest commits of the submodules.


## [0.5.4] - 2023-10-04
- Fix upstream remote of repositories when checking out.
- Add support for specifying the upstream origin (`myrepo.remote=upstream`).


## [0.5.3] - 2023-09-20
- Print commit hashes where relevant (`./repo status`, `./repo list`).


## [0.5.2] - 2023-09-09
- Support root repositories that are not on a branch.
- Support checking out specific hashes of submodules (`./repo checkout`) after checking out a particular root commit


## [0.5.1] - 2023-09-08
- Small bug fix for switching branches.


## [0.5.0] - 2023-09-08
- Add support for submodules (`myrepo.submodule=true`).
- Add support for specifying individual repositories to work on (`--repo myrepo`).
- Fix errors when repositories are not checked out.


## [0.4.9] - 2023-09-05
- Add `reset` command, which does a mixed reset to the branch by default.
  If `--hard` is specified, it does a hard reset.


## [0.4.8] - 2023-07-24
### Added
- Prints the commit hashes when performing a clone or update.

### Changed
- Project uses default Gradle wrapper location to allow easy import in IntelliJ.


## [0.4.7] - 2021-10-08
### Added
- Task `publishAllToMavenLocal` to publish all artifacts to the local Maven repository.

### Changed
- In the devenv repositories plugin, repository fetches now fetch from all remotes with `--all`.



[Unreleased]: https://github.com/metaborg/gradle.config/compare/release-0.5.7...HEAD
[0.5.7]: https://github.com/metaborg/gradle.config/compare/release-0.5.6...release-0.5.7
[0.5.6]: https://github.com/metaborg/gradle.config/compare/release-0.5.5...release-0.5.6
[0.5.5]: https://github.com/metaborg/gradle.config/compare/release-0.5.4...release-0.5.5
[0.5.4]: https://github.com/metaborg/gradle.config/compare/release-0.5.3...release-0.5.4
[0.5.3]: https://github.com/metaborg/gradle.config/compare/release-0.5.2...release-0.5.3
[0.5.2]: https://github.com/metaborg/gradle.config/compare/release-0.5.1...release-0.5.2
[0.5.1]: https://github.com/metaborg/gradle.config/compare/release-0.5.0...release-0.5.1
[0.5.0]: https://github.com/metaborg/gradle.config/compare/release-0.4.9...release-0.5.0
[0.4.9]: https://github.com/metaborg/gradle.config/compare/release-0.4.8...release-0.4.9
[0.4.8]: https://github.com/metaborg/gradle.config/compare/release-0.4.7...release-0.4.8
[0.4.7]: https://github.com/metaborg/gradle.config/compare/release-0.4.6...release-0.4.7
