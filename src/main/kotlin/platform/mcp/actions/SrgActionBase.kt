/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.mappings.Mappings
import com.demonwav.mcdev.platform.mixin.handlers.ShadowHandler
import com.demonwav.mcdev.util.ActionData
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.JComponent
import org.apache.commons.lang.StringEscapeUtils

abstract class SrgActionBase : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)

        if (data.element !is PsiIdentifier) {
            showBalloon("Not a valid element", e)
            return
        }

        val mcpModule = data.instance.getModuleOfType(McpModuleType) ?: return showBalloon("No mappings found", e)

        mcpModule.mappingsManager?.mappings?.onSuccess { srgMap ->
            var parent = data.element.parent ?: return@onSuccess showBalloon("Not a valid element", e)

            if (parent is PsiMember) {
                val shadowTarget = ShadowHandler.getInstance()?.findFirstShadowTargetForReference(parent)?.element
                if (shadowTarget != null) {
                    parent = shadowTarget
                }
            }

            if (parent is PsiReference) {
                parent = parent.resolve() ?: return@onSuccess showBalloon("Not a valid element", e)
            }

            withSrgTarget(parent, srgMap, e, data)
        }?.onError {
            showBalloon(it.message ?: "No MCP data available", e)
        } ?: showBalloon("No mappings found", e)
    }

    abstract fun withSrgTarget(parent: PsiElement, srgMap: Mappings, e: AnActionEvent, data: ActionData)

    companion object {
        fun showBalloon(message: String, e: AnActionEvent) {
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, null, LightColors.YELLOW, null)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()

            val project = e.project ?: return

            invokeLater {
                val element = getDataFromActionEvent(e)?.element
                val editor = getDataFromActionEvent(e)?.editor
                if (element != null && editor != null) {
                    val pos = editor.offsetToVisualPosition(element.textRange.endOffset - element.textLength / 2)
                    val at = RelativePoint(
                        editor.contentComponent,
                        editor.visualPositionToXY(VisualPosition(pos.line + 1, pos.column)),
                    )
                    balloon.show(at, Balloon.Position.below)
                    return@invokeLater
                }

                val statusBar = WindowManager.getInstance().getStatusBar(project)
                val statusBarComponent = statusBar.component
                if (statusBarComponent != null) {
                    balloon.show(RelativePoint.getCenterOf(statusBarComponent), Balloon.Position.below)
                    return@invokeLater
                }

                val focused = WindowManager.getInstance().getFocusedComponent(project)
                if (focused is JComponent) {
                    balloon.show(RelativePoint.getCenterOf(focused), Balloon.Position.below)
                    return@invokeLater
                }

                balloon.show(RelativePoint.fromScreen(Point()), Balloon.Position.below)
            }
        }

        fun showSuccessBalloon(editor: Editor, element: PsiElement, text: String) {
            val escapedText = StringEscapeUtils.escapeHtml(text)
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(escapedText, null, LightColors.SLIGHTLY_GREEN, null)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()

            invokeLater {
                val pos = editor.offsetToVisualPosition(element.textRange.endOffset - element.textLength / 2)
                val at = RelativePoint(
                    editor.contentComponent,
                    editor.visualPositionToXY(VisualPosition(pos.line + 1, pos.column)),
                )

                balloon.show(at, Balloon.Position.below)
            }
        }
    }
}
