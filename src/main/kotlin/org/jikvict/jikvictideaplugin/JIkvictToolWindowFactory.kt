package org.jikvict.jikvictideaplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class JIkvictToolWindowFactory: ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(UiMainPanel(project).createPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}