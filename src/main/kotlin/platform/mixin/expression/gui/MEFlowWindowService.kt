/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.expression.gui

import com.demonwav.mcdev.platform.mixin.util.shortName
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

private const val TOOL_WINDOW_ID = "MixinExtras Flow Diagram"
private val FLOW_DIAGRAM_KEY = Key.create<FlowDiagram>("${MEFlowWindowService::class.java.name}.flowDiagram")

@Service(Service.Level.PROJECT)
class MEFlowWindowService(private val project: Project, private val scope: CoroutineScope) {
    fun showDiagram(clazz: ClassNode, method: MethodNode, action: (FlowDiagram) -> Unit) {
        scope.launch(Dispatchers.EDT) {
            showDiagramImpl(clazz, method, action)
        }
    }

    private suspend fun showDiagramImpl(clazz: ClassNode, method: MethodNode, action: (FlowDiagram) -> Unit) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        var toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID)

        if (toolWindow == null) {
            // override-only API usage IJPL-224900
            toolWindow = toolWindowManager.registerToolWindow(TOOL_WINDOW_ID) {
                canCloseContent = true
                anchor = ToolWindowAnchor.RIGHT
                hideOnEmptyContent = true
            }
        }

        val content = chooseContent(toolWindow, clazz, method) ?: return Messages.showErrorDialog(
            project, "Failed to create flow diagram", "Error"
        )
        toolWindow.contentManager.setSelectedContent(content)
        toolWindow.activate {
            content.getUserData(FLOW_DIAGRAM_KEY)?.let(action)
        }
    }

    private suspend fun chooseContent(toolWindow: ToolWindow, clazz: ClassNode, method: MethodNode): Content? {
        val contentManager = toolWindow.contentManager
        val existing = contentManager.contents.find { it.getUserData(FLOW_DIAGRAM_KEY)?.method === method }
        existing?.let { return it }
        val content = createContent(clazz, method) ?: return null
        content.isCloseable = true
        contentManager.addContent(content)
        return content
    }

    private suspend fun createContent(clazz: ClassNode, method: MethodNode): Content? =
        withBackgroundProgress(project, "Creating Flow Diagram") compute@{
            val diagram = withContext(Dispatchers.Default) {
                FlowDiagram.create(project, scope, clazz, method)
            } ?: return@compute null
            val container = JPanel(BorderLayout())
            container.add(diagram.ui, BorderLayout.CENTER)
            val content = ContentFactory.getInstance().createContent(container, getTabName(clazz, method), false)
            content.putUserData(FLOW_DIAGRAM_KEY, diagram)
            content
        }

    private fun getTabName(clazz: ClassNode, method: MethodNode): String {
        return Type.getObjectType(clazz.name).shortName + "::" + method.name
    }
}
