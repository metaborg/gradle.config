package mb.gradle.config.devenv

import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import java.io.File

fun getCurrentGitBranch(directory: File): String {
  try {
    FileRepositoryBuilder().readEnvironment().findGitDir(directory).setMustExist(true).build()
  } catch(e: RepositoryNotFoundException) {
    throw GradleException("Cannot retrieve current branch name because no git repository was found at '$directory'", e)
  }.use { repo ->
    // Use repository with 'use' to close repository after use, freeing up resources.
    val headRef = repo.exactRef(Constants.HEAD)
      ?: throw GradleException("Cannot retrieve current branch name because repository has no HEAD")
    if(headRef.isSymbolic) {
      return Repository.shortenRefName(headRef.target.name)
    } else {
      throw GradleException("Cannot retrieve current branch name because repository HEAD is not symbolic")
    }
  }
}
