package org.jikvict.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

abstract class CleanUpSerializableTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @TaskAction
    fun clean() {
        inputDir
            .get()
            .asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val updatedText =
                    file
                        .readText()
                        .replace("@Serializable@Serializable", "@Serializable")
                        .replace("@KSerializable", "@Serializable")
                        .replace(" : Serializable", "")
                file.writeText(updatedText)
            }
    }
}
