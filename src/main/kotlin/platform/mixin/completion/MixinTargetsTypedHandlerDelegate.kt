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

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.reference.MixinReferences
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.util.PsiTreeUtil

class MixinTargetsTypedHandlerDelegate : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if ((charTyped == '$' || charTyped == '.') && file.language.isKindOf(JavaLanguage.INSTANCE)) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor) { f ->
                val offset = editor.caretModel.offset
                val element = f.findElementAt(offset)
                val literal = PsiTreeUtil.getParentOfType(element, PsiLiteral::class.java)
                literal != null && MixinReferences.MIXIN_TARGETS.accepts(literal)
            }
        }
        return Result.CONTINUE
    }
}
