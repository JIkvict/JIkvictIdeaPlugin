package org.jikvict.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.jikvict.gradle.tasks.CleanUpSerializableTask
import org.jikvict.gradle.tasks.GetOpenApiTask
import org.openapitools.generator.gradle.plugin.extensions.OpenApiGeneratorGenerateExtension

abstract class ExtendedOpenApiPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("org.openapi.generator")

        target.extensions.configure<OpenApiGeneratorGenerateExtension>("openApiGenerate") {
            generatorName.set("kotlin")
            library.set("multiplatform")
            inputSpec.set("${target.layout.buildDirectory.get()}/openapi.json")
            outputDir.set("${target.layout.buildDirectory.get()}/generated/openapi")
            packageName.set("org.jikvict.api")
            configOptions.set(
                mapOf(
                    "dateLibrary" to "string",
                    "serializationLibrary" to "kotlinx_serialization",
                    "parcelizeModels" to "false",
                    "withJava" to "false",
                    "dateTimeFormat" to "iso8601",
                    "exceptionOnFailure" to "true",
                    "exceptionOnHttpError" to "true"
                )
            )
        }


        with(target) {
            tasks.register<CleanUpSerializableTask>("cleanUpSerializable") {
                group = "build"
                description = "Clean up generated Kotlin files to remove unnecessary annotations and imports"
                inputDir.set(layout.buildDirectory.dir("generated/openapi/src"))
            }

            tasks.register<GetOpenApiTask>("getOpenApiJson") {
                repoUrl.set("https://github.com/JIkvict/JIkvictBackend.git")
                branch.set("docs")
                version.set("latest")
                outputFile.set(layout.buildDirectory.file("openapi.json"))
            }

            tasks.named("openApiGenerate") {
                dependsOn("getOpenApiJson")
                finalizedBy("cleanUpSerializable")
            }
        }
    }
}