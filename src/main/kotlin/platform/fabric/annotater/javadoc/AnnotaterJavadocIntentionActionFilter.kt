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
import com.intellij.psi.PsiFile
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
        return listOf(offset, offset - 1).any { candidate ->
            val element = psiFile.findElementAt(candidate) ?: return@any false
            val comment = PsiTreeUtil.getParentOfType(element, PsiDocComment::class.java, false) ?: return@any false
            !AnnotaterJavadocInlineEditor.isGuarded(document, comment.textRange)
        }
    }
}
