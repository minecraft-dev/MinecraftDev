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

import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
import com.demonwav.mcdev.util.ActionData
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyAtAction : SrgActionBase() {
    override fun withSrgTarget(parent: PsiElement, srgMap: McpSrgMap, e: AnActionEvent, data: ActionData) {
        when (parent) {
            is PsiField -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getSrgClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getSrgField(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    classSrg + " " + srg.name + " # " + parent.name
                )
            }
            is PsiMethod -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getSrgClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getSrgMethod(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    classSrg + " " + srg.name + srg.descriptor + " # " + parent.name
                )
            }
            is PsiClass -> {
                val classMcpToSrg = srgMap.getSrgClass(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(data.editor, data.element, classMcpToSrg)
            }
            else -> showBalloon("Not a valid element", e)
        }
    }

    private fun copyToClipboard(editor: Editor, element: PsiElement, text: String) {
        val stringSelection = StringSelection(text)
        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
        clpbrd.setContents(stringSelection, null)
        showSuccessBalloon(editor, element, "Copied $text")
    }
}
