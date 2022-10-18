/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
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

abstract class SrgActionBase : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)

        if (data.element !is PsiIdentifier) {
            showBalloon("Not a valid element", e)
            return
        }

        val mcpModule = data.instance.getModuleOfType(McpModuleType) ?: return showBalloon("No mappings found", e)

        mcpModule.srgManager?.srgMap?.onSuccess { srgMap ->
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

    abstract fun withSrgTarget(parent: PsiElement, srgMap: McpSrgMap, e: AnActionEvent, data: ActionData)

    companion object {
        fun showBalloon(message: String, e: AnActionEvent) {
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, null, LightColors.YELLOW, null)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()

            val project = e.project ?: return
            val statusBar = WindowManager.getInstance().getStatusBar(project)

            invokeLater {
                val element = getDataFromActionEvent(e)?.element
                val editor = getDataFromActionEvent(e)?.editor
                val at = if (element != null && editor != null) {
                    val pos = editor.offsetToVisualPosition(element.textRange.endOffset - element.textLength / 2)
                    RelativePoint(
                        editor.contentComponent,
                        editor.visualPositionToXY(VisualPosition(pos.line + 1, pos.column))
                    )
                } else RelativePoint.getCenterOf(statusBar.component)
                balloon.show(at, Balloon.Position.below)
            }
        }

        fun showSuccessBalloon(editor: Editor, element: PsiElement, text: String) {
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, null, LightColors.SLIGHTLY_GREEN, null)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()

            invokeLater {
                val pos = editor.offsetToVisualPosition(element.textRange.endOffset - element.textLength / 2)
                val at = RelativePoint(
                    editor.contentComponent,
                    editor.visualPositionToXY(VisualPosition(pos.line + 1, pos.column))
                )

                balloon.show(at, Balloon.Position.below)
            }
        }
    }
}
