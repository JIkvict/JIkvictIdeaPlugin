package org.jikvict.jikvictideaplugin.services

import com.intellij.openapi.application.ApplicationManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jikvict.api.apis.AssignmentControllerApi
import org.jikvict.api.models.AssignmentDto
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class AssignmentService(private val api: AssignmentControllerApi? = null) {
    companion object {
        const val BASE_URL = "https://jikvict.fiiture.sk"
    }

    private fun getApi(): AssignmentControllerApi {
        if (api != null) return api
        val token = SettingsState.getInstance().jwtToken
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(DefaultRequest) {
                headers.append("Authorization", "Bearer $token")
            }
        }
        return AssignmentControllerApi(
            baseUrl = BASE_URL,
            httpClient = httpClient
        )
    }

    private fun getToken(): String = SettingsState.getInstance().jwtToken

    suspend fun submitAssignment(
        assignment: AssignmentDto,
        project: com.intellij.openapi.project.Project,
        onStatusUpdate: (org.jikvict.api.models.PendingStatusResponseLong) -> Unit = {}
    ) {
        println("[JIkvict] Starting submission for assignment ${assignment.id} (${assignment.title})")

        val projectDir = File(project.basePath ?: error("Project path is not available"))
        println("[JIkvict] Using current project directory: ${projectDir.absolutePath}")

        val metaFile = File(projectDir, ".jikvict-meta.json")
        val taskDir = if (metaFile.exists() && projectDir.name.startsWith("task")) {
            println("[JIkvict] Using project directory as task directory (meta file found)")
            projectDir
        } else {
            println("[JIkvict] Searching for task directory...")
            findTaskDir(projectDir) ?: error("Task directory not found")
        }
        println("[JIkvict] Task directory: ${taskDir.absolutePath}")

        val zipFile = File.createTempFile("submission", ".zip")
        println("[JIkvict] Creating zip archive at: ${zipFile.absolutePath}")
        zipDir(taskDir, zipFile)
        println("[JIkvict] Zip created successfully, size: ${zipFile.length()} bytes")

        val token = getToken()
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(DefaultRequest) {
                headers.append("Authorization", "Bearer $token")
            }
        }

        println("[JIkvict] Uploading submission to $BASE_URL/api/v1/solution-checker/submit")
        val response = client.post("$BASE_URL/api/v1/solution-checker/submit") {
            header("Authorization", "Bearer $token")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = InputProvider {
                                zipFile.inputStream().use { input ->
                                    buildPacket { writeFully(input.readBytes()) }
                                }
                            },
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"submission.zip\"")
                            }
                        )
                        append("assignmentId", assignment.id.toString())
                    }
                )
            )
            contentType(ContentType.MultiPart.FormData)
        }

        zipFile.delete()
        println("[JIkvict] Temporary zip file cleaned up")

        if (!response.status.isSuccess()) {
            println("[JIkvict] Submission failed with status: ${response.status}")
            println("[JIkvict] Submission failed: ${response.bodyAsText()}")
            client.close()
            error("Submit failed: ${response.status}")
        }


        val submitResponse: org.jikvict.api.models.PendingStatusResponseLong = response.body()
        println("[JIkvict] Upload successful, received taskId: ${submitResponse.data}")

        val taskId = submitResponse.data ?: run {
            client.close()
            error("No taskId received from server")
        }


        val statusApi = org.jikvict.api.apis.TaskStatusControllerApi(
            baseUrl = BASE_URL,
            httpClient = client
        )


        println("[JIkvict] Starting status polling for taskId: $taskId")
        var currentStatus: org.jikvict.api.models.PendingStatusResponseLong
        do {
            kotlinx.coroutines.delay(1500)
            currentStatus = statusApi.getTaskStatus(taskId).body()
            println("[JIkvict] Status: ${currentStatus.status}, Message: ${currentStatus.message}")
            onStatusUpdate(currentStatus)
        } while (currentStatus.status == org.jikvict.api.models.PendingStatusResponseLong.Status.PENDING)

        println("[JIkvict] Submission processing completed with status: ${currentStatus.status}")
        client.close()
    }

    suspend fun downloadAndOpenProject(assignment: AssignmentDto) {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        val token = getToken()
        val response = client.get("$BASE_URL/api/assignment/zip/${assignment.id}") {
            header("Authorization", "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.OctetStream.toString())
        }
        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.body<String>()
            } catch (e: Exception) {
                "Unable to read error details"
            }
            client.close()
            error("Download failed: ${response.status.value} ${response.status.description}. Details: $errorBody")
        }

        try {
            val tempDir = Files.createTempDirectory("jikvict_temp").toFile()

            val zipFile = File(tempDir, "project.zip")
            FileOutputStream(zipFile).use { out ->
                val bytes: ByteArray = response.body()
                out.write(bytes)
            }


            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }


            zipFile.delete()


            val taskDir = tempDir.listFiles()?.firstOrNull {
                it.isDirectory && it.name.startsWith("task")
            } ?: error("Task directory not found in archive")


            val metaFile = File(taskDir, ".jikvict-meta.json")
            val metaContent = """
                {
                  "taskId": ${assignment.taskId},
                  "assignmentId": ${assignment.id}
                }
            """.trimIndent()
            metaFile.writeText(metaContent)


            ApplicationManager.getApplication().invokeLater {
                com.intellij.ide.impl.ProjectUtil.openOrImport(taskDir.toPath(), null, true)
            }
        } finally {
            client.close()
        }
    }

    private fun findTaskDir(root: File): File? {
        val candidates = listOf("task", "src", "solution")
        return candidates.asSequence()
            .map { File(root, it) }
            .firstOrNull { it.exists() && it.isDirectory }
            ?: root.takeIf { it.exists() && it.isDirectory }
    }

    private fun zipDir(sourceDir: File, zip: File) {
        val excludedDirs = setOf("build", ".gradle", ".idea", "out", ".git", "target")

        java.util.zip.ZipOutputStream(zip.outputStream()).use { zos ->
            fun addFile(file: File, basePath: String) {
                val entryName = file.path.removePrefix(basePath).trimStart(File.separatorChar)


                if (file.isDirectory && excludedDirs.contains(file.name)) {
                    println("[JIkvict] Skipping directory: ${file.name}")
                    return
                }

                if (file.isDirectory) {
                    file.listFiles()?.forEach { addFile(it, basePath) }
                } else {
                    zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            addFile(sourceDir, sourceDir.parentFile.absolutePath + File.separator)
        }
    }

    suspend fun loadAssignments(): List<AssignmentDto> = withContext(Dispatchers.IO) {
        getApi().getAll().body()
    }
}
