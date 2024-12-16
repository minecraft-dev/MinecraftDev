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

package com.demonwav.mcdev.platform.mcp.aw.fixes

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings

class RemoveAwEntryFix(startElement: PsiElement, endElement: PsiElement, val inBatchMode: Boolean) :
    LocalQuickFixOnPsiElement(startElement, endElement) {

    override fun getFamilyName(): @IntentionFamilyName String = "Remove entry"

    override fun getText(): @IntentionName String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        startElement.parent.deleteChildRange(startElement, endElement)
    }

    override fun availableInBatchMode(): Boolean = inBatchMode

    companion object {

        fun forWholeLine(entry: AwEntry, inBatchMode: Boolean): RemoveAwEntryFix {
            val start = entry.siblings(forward = false, withSelf = false)
                .firstOrNull { it.elementType == AwTypes.CRLF }?.nextSibling
            val end = entry.siblings(forward = true, withSelf = true)
                .firstOrNull { it.elementType == AwTypes.CRLF }
            return RemoveAwEntryFix(start ?: entry, end ?: entry, inBatchMode)
        }
    }
}
