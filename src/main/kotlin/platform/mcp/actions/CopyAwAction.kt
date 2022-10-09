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

import com.demonwav.mcdev.platform.mcp.actions.SrgActionBase.Companion.showBalloon
import com.demonwav.mcdev.platform.mcp.actions.SrgActionBase.Companion.showSuccessBalloon
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.demonwav.mcdev.util.internalName
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyAwAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return showBalloon("Unknown failure", e)
        val editor = data.editor

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

        doCopy(target, element, editor, e)
    }

    companion object {

        fun doCopy(target: PsiElement, element: PsiElement, editor: Editor?, e: AnActionEvent?) {
            when (target) {
                is PsiClass -> {
                    val text = "accessible class ${target.internalName}"
                    copyToClipboard(editor, element, text)
                }
                is PsiField -> {
                    val containing = target.containingClass?.internalName
                        ?: return maybeShow("Could not get owner of field", e)
                    val desc = target.type.descriptor
                    val text = "accessible field $containing ${target.name} $desc"
                    copyToClipboard(editor, element, text)
                }
                is PsiMethod -> {
                    val containing = target.containingClass?.internalName
                        ?: return maybeShow("Could not get owner of method", e)
                    val desc = target.descriptor ?: return maybeShow("Could not get descriptor of method", e)
                    val text = "accessible method $containing ${target.name} $desc"
                    copyToClipboard(editor, element, text)
                }
                else -> maybeShow("Invalid element", e)
            }
        }

        private fun copyToClipboard(editor: Editor?, element: PsiElement, text: String) {
            val stringSelection = StringSelection(text)
            val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
            clpbrd.setContents(stringSelection, null)
            if (editor != null) {
                showSuccessBalloon(editor, element, "Copied: \"$text\"")
            }
        }

        private fun maybeShow(text: String, e: AnActionEvent?) {
            if (e != null) {
                showBalloon(text, e)
            }
        }
    }
}
