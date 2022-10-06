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

import com.demonwav.mcdev.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.*
import com.intellij.ui.LightColors
import com.intellij.ui.awt.RelativePoint
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyAwAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)

        val element = data.element
        if (element !is PsiIdentifier) {
            showBalloon("Invalid element", e)
            return
        }

        val target = when (val parent = element.parent) {
            is PsiMember -> parent
            is PsiReference -> parent.resolve()
            else -> null
        } ?: return showBalloon("Invalid element", e)

        when (target) {
            is PsiClass -> {
                val text = "accessible class ${target.internalName}"
                copyToClipboard(data.editor, element, text)
            }
            is PsiField -> {
                val containing = target.containingClass?.internalName ?: return showBalloon("Could not get owner of field", e)
                val desc = target.type.descriptor
                val text = "accessible field $containing ${target.name} $desc"
                copyToClipboard(data.editor, element, text)
            }
            is PsiMethod -> {
                val containing = target.containingClass?.internalName ?: return showBalloon("Could not get owner of method", e)
                val desc = target.descriptor ?: return showBalloon("Could not get descriptor of method", e)
                val text = "accessible method $containing ${target.name} $desc"
                copyToClipboard(data.editor, element, text)
            }
            else -> showBalloon("Invalid element", e)
        }
    }

    // TODO: deduplicate code with SrgActionBase

    private fun showBalloon(message: String, e: AnActionEvent) {
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
			val at = if(element != null && editor != null) {
				val pos = editor.offsetToVisualPosition(element.textRange.endOffset - element.textLength / 2)
				RelativePoint(
					editor.contentComponent,
					editor.visualPositionToXY(VisualPosition(pos.line + 1, pos.column))
				)
			} else RelativePoint.getCenterOf(statusBar.component)
            balloon.show(at, Balloon.Position.below)
        }
    }

    private fun copyToClipboard(editor: Editor, element: PsiElement, text: String) {
        val stringSelection = StringSelection(text)
        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
        clpbrd.setContents(stringSelection, null)
        showSuccessBalloon(editor, element, "Copied: \"$text\"")
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
                Balloon.Position.below
            )
        }
    }
}
