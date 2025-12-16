package org.jikvict.jikvictideaplugin.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jikvict.jikvictideaplugin.services.ProjectMetaService
import java.io.File

@Serializable
data class ProjectMeta(
    val taskId: Long,
    val assignmentId: Long
)

class ProjectMetaDetector : ProjectActivity {
    override suspend fun execute(project: Project) {
        val basePath = project.basePath ?: return
        val metaFile = File(basePath, ".jikvict-meta.json")
        
        if (metaFile.exists()) {
            try {
                val metaContent = metaFile.readText()
                val meta = Json.decodeFromString<ProjectMeta>(metaContent)
                ProjectMetaService.getInstance(project).setMeta(meta)
            } catch (e: Exception) {
                
            }
        }
    }
}
