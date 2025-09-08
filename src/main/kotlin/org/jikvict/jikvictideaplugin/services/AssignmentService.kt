package org.jikvict.jikvictideaplugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.vfs.VfsUtil
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jikvict.api.apis.AssignmentControllerApi
import org.jikvict.api.models.AssignmentDto
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class AssignmentService(private val api: AssignmentControllerApi = AssignmentControllerApi()) {
    suspend fun submitAssignment(assignment: AssignmentDto) {
        val projectDir = withContext(Dispatchers.IO) {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "Select Project Directory"
            val vf = FileChooser.chooseFile(descriptor, null, null)
                ?: error("No directory selected")
            VfsUtil.virtualToIoFile(vf)
        }

        val taskDir = findTaskDir(projectDir) ?: error("Task directory not found")

        val zipFile = File.createTempFile("submission", ".zip")
        zipDir(taskDir, zipFile)

        val client = HttpClient()
        val token = System.getenv("AUTHORIZATION") ?: ""

        val response = client.post("http://localhost:8080/api/assignments/${assignment.id}/attempts") {
            header("Authorization", token)
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
                    }
                )
            )
            contentType(ContentType.MultiPart.FormData)
        }
        if (!response.status.isSuccess()) error("Submit failed: ${response.status}")
    }

    suspend fun downloadAndOpenProject(assignment: AssignmentDto) {
        val client = HttpClient()
        val token = System.getenv("AUTHORIZATION") ?: ""
        val response = client.get("http://localhost:8080/api/assignments/${assignment.id}/download") {
            header("Authorization", token)
        }
        if (!response.status.isSuccess()) error("Download failed: ${response.status}")

        val tempDir = Files.createTempDirectory("assignment_${assignment.id}").toFile()
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

        val projectDir = tempDir.listFiles()?.firstOrNull { it.isDirectory } ?: tempDir
        ApplicationManager.getApplication().invokeLater {
            com.intellij.ide.impl.ProjectUtil.openOrImport(projectDir.toPath(), null, true)
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
        java.util.zip.ZipOutputStream(zip.outputStream()).use { zos ->
            fun addFile(file: File, basePath: String) {
                val entryName = file.path.removePrefix(basePath).trimStart(File.separatorChar)
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
        api.getAll().body()
    }
}
