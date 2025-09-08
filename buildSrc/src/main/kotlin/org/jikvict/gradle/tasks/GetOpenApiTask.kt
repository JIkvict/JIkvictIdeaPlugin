package org.jikvict.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jikvict.gradle.logic.GithubRetriever

abstract class GetOpenApiTask : DefaultTask() {
    @get:Input
    abstract val repoUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val branch: Property<String>

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @Transient
    private var cachedRemoteJson: String? = null

    init {
        outputFile.convention(project.layout.buildDirectory.file("openapi.json"))

        onlyIf {
            val outFile = outputFile.get().asFile
            val current = if (outFile.exists()) outFile.readText() else null
            val remote = fetchRemoteJson()
            cachedRemoteJson = remote
            current != remote
        }
    }

    @TaskAction
    fun execute() {
        val json = cachedRemoteJson ?: fetchRemoteJson()
        outputFile.get().asFile.writeText(json)
    }

    private fun fetchRemoteJson(): String {
        val url = repoUrl.get()
        val branchName = branch.orNull ?: "docs"
        val ver = version.orNull ?: "latest"
        val retriever = GithubRetriever(
            version = ver,
            repoUrl = url,
            branch = branchName
        )
        return retriever.getOpenApiJson()
    }
}
