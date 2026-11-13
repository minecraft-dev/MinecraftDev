/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.codeInsight.daemon.impl.IntentionActionFilter
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionDelegate
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.modcommand.ActionContext
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil

/**
 * Hides intentions and quick fixes in the copies of Minecraft source files that annotater Javadocs are edited in,
 * except in the edited Javadoc comment. Guarded blocks only stop the user from typing in the hidden source code, not
 * other changes to the document, and fixes that apply to the whole file (with an offset of -1) could change it.
 */
class AnnotaterJavadocIntentionActionFilter : IntentionActionFilter {
    override fun accept(intentionAction: IntentionAction, psiFile: PsiFile?, offset: Int): Boolean {
        if (psiFile == null || AnnotaterJavadocVirtualFile.of(psiFile) == null) {
            return true
        }
        if (offset < 0) {
            return false
        }
        val document = psiFile.viewProvider.document
        // The caret can be just after the end of a comment
        val comment = listOf(offset, offset - 1).firstNotNullOfOrNull { candidate ->
            val element = psiFile.findElementAt(candidate) ?: return@firstNotNullOfOrNull null
            PsiTreeUtil.getParentOfType(element, PsiDocComment::class.java, false)
                ?.takeUnless { AnnotaterJavadocInlineEditor.isGuarded(document, it.textRange) }
        } ?: return false
        return !isDeclarationIntention(intentionAction, psiFile, comment)
    }

    /**
     * Java offers some intentions of a declaration anywhere in its doc comment, such as changing its access modifier.
     * These are told apart from the doc comment's own intentions by also being available on the declaration's name.
     */
    private fun isDeclarationIntention(action: IntentionAction, psiFile: PsiFile, comment: PsiDocComment): Boolean {
        if (!isRegisteredIntention(action)) {
            // keep quick fixes enabled
            return false
        }
        val name = (comment.owner as? PsiNameIdentifierOwner)?.nameIdentifier ?: return false
        val editor = NameEditor(psiFile)
        editor.caretModel.moveToOffset(name.textRange.startOffset)
        val unwrapped = IntentionActionDelegate.unwrap(action)
        val modCommand = unwrapped.asModCommandAction()
        return try {
            when {
                unwrapped is PsiElementBaseIntentionAction ->
                    unwrapped.checkFile(psiFile) && unwrapped.isAvailable(psiFile.project, editor, name)
                modCommand != null -> modCommand.getPresentation(ActionContext.from(editor, psiFile)) != null
                else -> unwrapped.isAvailable(psiFile.project, editor, psiFile)
            }
        } catch (_: UnsupportedOperationException) {
            // The intention needs more of an editor than this has
            false
        }
    }

    private class NameEditor(psiFile: PsiFile) : ImaginaryEditor(psiFile.project, psiFile.viewProvider.document) {
        override fun isViewer() = false

        override fun isOneLineMode() = false
    }

    private fun isRegisteredIntention(action: IntentionAction): Boolean {
        val unwrapped = unwrap(action)
        return IntentionManager.getInstance().intentionActions.any { unwrap(it) === unwrapped }
    }

    private fun unwrap(action: IntentionAction): Any {
        val unwrapped = IntentionActionDelegate.unwrap(action)
        return unwrapped.asModCommandAction() ?: unwrapped
    }
}
