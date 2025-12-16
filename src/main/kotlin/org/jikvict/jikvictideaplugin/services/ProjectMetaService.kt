package org.jikvict.jikvictideaplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jikvict.jikvictideaplugin.listeners.ProjectMeta

@Service(Service.Level.PROJECT)
class ProjectMetaService {
    private var meta: ProjectMeta? = null

    fun setMeta(projectMeta: ProjectMeta) {
        meta = projectMeta
    }

    fun getMeta(): ProjectMeta? = meta

    fun hasMeta(): Boolean = meta != null

    companion object {
        fun getInstance(project: Project): ProjectMetaService = project.service()
    }
}
