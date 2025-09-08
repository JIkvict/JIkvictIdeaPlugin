package org.jikvict.gradle.logic

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import java.nio.file.Files

class GithubRetriever(
    val version: String = "latest",
    val repoUrl: String,
    val branch: String = "docs"
) {

    @Suppress("NewApi")
    fun getOpenApiJson(): String {
        val repoDesc = DfsRepositoryDescription("streaming-repo")
        val repo = InMemoryRepository(repoDesc)
        repo.create()

        val tempDir = Files.createTempDirectory("repo")

        Git.cloneRepository()
            .setURI(repoUrl)
            .setDirectory(tempDir.toFile())
            .setBranch(branch)
            .setDepth(1)
            .call()

        val formattedVersion = if (version == "latest") version else "v$version"

        val content = tempDir.resolve(formattedVersion).resolve("openapi.json").toFile().readText()

        return content
    }

}