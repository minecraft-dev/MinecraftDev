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

import com.demonwav.mcdev.platform.mcp.actions.SrgActionBase.Companion.showBalloon
import com.demonwav.mcdev.platform.mcp.actions.SrgActionBase.Companion.showSuccessBalloon
import com.demonwav.mcdev.platform.mcp.at.usesSrgMemberNames
import com.demonwav.mcdev.platform.mixin.handlers.ShadowHandler
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.getDataFromActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class CopyNeoForgeAtAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isAvailable(e)
    }

    private fun isAvailable(e: AnActionEvent): Boolean {
        val data = getDataFromActionEvent(e) ?: return false
        return !data.instance.usesSrgMemberNames()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val data = getDataFromActionEvent(e) ?: return

        var parent = data.element.parent
        if (parent is PsiMember) {
            val shadowTarget = ShadowHandler.getInstance()?.findFirstShadowTargetForReference(parent)?.element
            if (shadowTarget != null) {
                parent = shadowTarget
            }
        }

        if (parent is PsiReference) {
            parent = parent.resolve() ?: return showBalloon("Not a valid element", e)
        }

        when (parent) {
            is PsiClass -> {
                val fqn = parent.qualifiedName ?: return showBalloon("Could not find class FQN", e)
                copyToClipboard(data.editor, data.element, fqn)
            }
            is PsiField -> {
                val classFqn = parent.containingClass?.qualifiedName
                    ?: return showBalloon("Could not find class FQN", e)
                copyToClipboard(data.editor, data.element, "$classFqn ${parent.name}")
            }
            is PsiMethod -> {
                val classFqn = parent.containingClass?.qualifiedName
                    ?: return showBalloon("Could not find class FQN", e)
                val methodDescriptor = parent.descriptor
                    ?: return showBalloon("Could not compute method descriptor", e)
                copyToClipboard(data.editor, data.element, "$classFqn ${parent.name}$methodDescriptor")
            }
            else -> showBalloon("Not a valid element", e)
        }
        return
    }

    private fun copyToClipboard(editor: Editor, element: PsiElement, text: String) {
        val stringSelection = StringSelection(text)
        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
        clpbrd.setContents(stringSelection, null)
        showSuccessBalloon(editor, element, "Copied $text")
    }
}
