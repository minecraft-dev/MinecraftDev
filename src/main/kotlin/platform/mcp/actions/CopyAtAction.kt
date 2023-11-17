/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.demonwav.mcdev.platform.mcp.mappings.Mappings
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
    override fun withSrgTarget(parent: PsiElement, srgMap: Mappings, e: AnActionEvent, data: ActionData) {
        when (parent) {
            is PsiField -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getIntermediaryClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getIntermediaryField(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    classSrg + " " + srg.name + " # " + parent.name,
                )
            }
            is PsiMethod -> {
                val containing = parent.containingClass ?: return showBalloon("No SRG name found", e)
                val classSrg = srgMap.getIntermediaryClass(containing) ?: return showBalloon("No SRG name found", e)
                val srg = srgMap.getIntermediaryMethod(parent) ?: return showBalloon("No SRG name found", e)
                copyToClipboard(
                    data.editor,
                    data.element,
                    classSrg + " " + srg.name + srg.descriptor + " # " + parent.name,
                )
            }
            is PsiClass -> {
                val classMcpToSrg = srgMap.getIntermediaryClass(parent) ?: return showBalloon("No SRG name found", e)
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
