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

package com.demonwav.mcdev.platform.mcp.at.inspections

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.AtFile
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class AtInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        val entry = element.parentOfType<AtEntry>(withSelf = true) ?: return false
        val entryComment = entry.commentText
        if (entryComment != null) {
            if (isSuppressing(entryComment, toolId)) {
                return true
            }
        }

        val file = element.containingFile as AtFile
        return file.headComments.any { comment -> isSuppressing(comment.text, toolId) }
    }

    private fun isSuppressing(entryComment: String, toolId: String): Boolean {
        val suppressed = entryComment.substringAfter("Suppress:").substringBefore(' ').split(',')
        return toolId in suppressed
    }

    override fun getSuppressActions(
        element: PsiElement?,
        toolId: String
    ): Array<out SuppressQuickFix> {
        if (element == null) {
            return SuppressQuickFix.EMPTY_ARRAY
        }

        val entry = element as? AtEntry
            ?: element.parentOfType<AtEntry>(withSelf = true)
            ?: PsiTreeUtil.getPrevSiblingOfType(element, AtEntry::class.java) // For when we are at a CRLF
        return if (entry != null) {
            arrayOf(AtSuppressQuickFix(entry, toolId), AtSuppressQuickFix(element.containingFile, toolId))
        } else {
            arrayOf(AtSuppressQuickFix(element.containingFile, toolId))
        }
    }

    class AtSuppressQuickFix(element: PsiElement, val toolId: String) :
        LocalQuickFixOnPsiElement(element), SuppressQuickFix {

        override fun getText(): @IntentionName String = when (startElement) {
            is AtEntry -> "Suppress $toolId for entry"
            is AtFile -> "Suppress $toolId for file"
            else -> "Suppress $toolId"
        }

        override fun getFamilyName(): @IntentionFamilyName String = "Suppress inspection"

        override fun invoke(
            project: Project,
            file: PsiFile,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            when (startElement) {
                is AtEntry -> suppressForEntry(startElement)
                is AtFile -> suppressForFile(startElement)
            }
        }

        private fun suppressForEntry(entry: AtEntry) {
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
            val newComment =
                commentText.substring(suppressStart, suppressEnd) + ",$toolId" + commentText.substring(suppressEnd)
            entry.setComment(newComment)
        }

        private fun suppressForFile(file: AtFile) {
            val existingSuppressComment = file.headComments.firstOrNull { it.text.contains("Suppress:") }
            if (existingSuppressComment == null) {
                file.addHeadComment("Suppress:$toolId")
                return
            }

            val commentText = existingSuppressComment.text
            val suppressStart = commentText.indexOf("Suppress:")
            if (suppressStart == -1) {
                file.addHeadComment("Suppress:$toolId")
                return
            }

            val suppressEnd = commentText.indexOf(' ', suppressStart).takeUnless { it == -1 } ?: commentText.length
            val newCommentText =
                commentText.substring(suppressStart, suppressEnd) + ",$toolId" + commentText.substring(suppressEnd)
            val newComment = AtElementFactory.createComment(file.project, newCommentText)
            existingSuppressComment.replace(newComment)
        }

        override fun isAvailable(
            project: Project,
            context: PsiElement
        ): Boolean = context.isValid

        override fun isSuppressAll(): Boolean = false
    }
}
