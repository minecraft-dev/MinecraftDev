/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.actions

import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mixin.util.findFirstShadowTarget
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.invokeLater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection



class CopyATAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)

        if (data.element !is PsiIdentifier) {
            showBalloon("Not a valid element", e)
            return
        }

        val mcpModule = data.instance.getModuleOfType(McpModuleType) ?: return showBalloon("No mappings found", e)

        mcpModule.srgManager?.srgMap?.done { srgMap ->
            var parent = data.element.parent

            if (parent is PsiMember) {
                val shadowTarget = parent.findFirstShadowTarget()
                if (shadowTarget != null) {
                    parent = shadowTarget
                }
            }

            if (parent is PsiReference) {
                parent = parent.resolve()
            }

            when (parent) {
                is PsiField -> {
                    val srg = srgMap.findSrgField(parent) ?: return@done showBalloon("No SRG name found", e)
                    copyToClipboard(data.editor, data.element, parent.containingClass?.qualifiedName + " " + srg.name + " #"+parent.name)
                }
                is PsiMethod -> {
                    val srg = srgMap.findSrgMethod(parent) ?: return@done showBalloon("No SRG name found", e)
                    copyToClipboard(data.editor, data.element, parent.containingClass?.qualifiedName + " " + srg.name + srg.descriptor+ " #"+parent.name)
                }
                is PsiClass -> {
                    val classMcpToSrg = srgMap.findSrgClass(parent) ?: return@done showBalloon("No SRG name found", e)
                    copyToClipboard(data.editor, data.element, classMcpToSrg)
                }
                else ->
                    showBalloon("Not a valid element", e)
            }
        } ?: showBalloon("No mappings found", e)
    }

    private fun showBalloon(message: String, e: AnActionEvent) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, LightColors.YELLOW, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        val statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.dataContext))

        invokeLater { balloon.show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight) }
    }

    private fun copyToClipboard(editor: Editor, element: PsiElement, text: String) {
        val stringSelection = StringSelection(text)
        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
        clpbrd.setContents(stringSelection, null)
        showSuccessBalloon(editor, element, "Copied "+text)
    }

    private fun showSuccessBalloon(editor: Editor, element: PsiElement, text: String) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(text, null, LightColors.SLIGHTLY_GREEN, null)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        invokeLater {
            balloon.show(
                    RelativePoint(
                            editor.contentComponent,
                            editor.visualPositionToXY(editor.offsetToVisualPosition(element.textRange.endOffset))
                    ),
                    Balloon.Position.atRight
            )
        }
    }
}