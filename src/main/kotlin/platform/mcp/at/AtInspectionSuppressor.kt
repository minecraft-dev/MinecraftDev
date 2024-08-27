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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType

class AtInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        val entry = element.parentOfType<AtEntry>(withSelf = true) ?: return false
        val comment = entry.commentText ?: return false
        val suppressed = comment.substringAfter("Suppress:").substringBefore(' ').split(',')
        return toolId in suppressed
    }

    override fun getSuppressActions(
        element: PsiElement?,
        toolId: String
    ): Array<out SuppressQuickFix> {
        if (element == null) {
            return SuppressQuickFix.EMPTY_ARRAY
        }

        return arrayOf(AtSuppressQuickFix(element, toolId))
    }

    class AtSuppressQuickFix(element: PsiElement, val toolId: String) : LocalQuickFixOnPsiElement(element), SuppressQuickFix {

        override fun getText(): @IntentionName String = "Suppress $toolId"

        override fun getFamilyName(): @IntentionFamilyName String = "Suppress inspection"

        override fun invoke(
            project: Project,
            file: PsiFile,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val entry = startElement.parentOfType<AtEntry>(withSelf = true) ?: return
            val commentText = entry.commentText?.trim()
            if (commentText == null) {
                entry.setComment("Suppress:$toolId")
                return
            }

            val suppressStart = commentText.indexOf("Suppress:")
            if (suppressStart == -1) {
                entry.setComment("Suppress:$toolId $commentText")
                return
            }

            val suppressEnd = commentText.indexOf(' ', suppressStart).takeUnless { it == -1 } ?: commentText.length
            val newComment = commentText.substring(suppressStart, suppressEnd) + ",$toolId" + commentText.substring(suppressEnd)
            entry.setComment(newComment)
        }

        override fun isAvailable(
            project: Project,
            context: PsiElement
        ): Boolean = context.isValid

        override fun isSuppressAll(): Boolean = false
    }
}
