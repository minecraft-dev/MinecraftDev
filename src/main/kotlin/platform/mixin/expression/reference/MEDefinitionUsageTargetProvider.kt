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

package com.demonwav.mcdev.platform.mixin.expression.reference

import com.demonwav.mcdev.util.findContainingNameValuePair
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageTargetProvider

class MEDefinitionUsageTargetProvider : UsageTargetProvider {
    override fun getTargets(element: PsiElement): Array<UsageTarget>? {
        return if (MEReferenceUtil.isDefinitionId(element)) {
            arrayOf(PsiElement2UsageTargetAdapter(element, true))
        } else {
            null
        }
    }

    override fun getTargets(editor: Editor, file: PsiFile): Array<UsageTarget>? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.document, offset)) ?: return null
        val nameValuePair = element.findContainingNameValuePair() ?: return null
        val value = nameValuePair.value ?: return null
        if (!PsiTreeUtil.isAncestor(value, element, false)) {
            return null
        }
        return if (MEReferenceUtil.isDefinitionId(value)) {
            arrayOf(PsiElement2UsageTargetAdapter(value, true))
        } else {
            null
        }
    }
}
