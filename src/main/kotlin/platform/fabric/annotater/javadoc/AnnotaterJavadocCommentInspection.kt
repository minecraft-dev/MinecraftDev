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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.McdevReflection
import com.demonwav.mcdev.util.McdevReflection.newInstanceUnwrap
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil

/**
 * Annotater only supports Markdown (`///`) Javadoc comments.
 */
class AnnotaterJavadocCommentInspection : LocalInspectionTool() {
    override fun getStaticDescription() = MCDevBundle("fabric.annotater.inspection.comment.description")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (AnnotaterJavadocVirtualFile.of(holder.file) == null) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        val document = holder.file.viewProvider.document

        // Comments in the read-only parts of the copied source file were already there
        fun isOriginal(comment: PsiComment) = AnnotaterJavadocInlineEditor.isGuarded(document, comment.textRange)

        return object : JavaElementVisitor() {
            override fun visitDocComment(comment: PsiDocComment) {
                if (comment.isMarkdownComment || isOriginal(comment)) {
                    return
                }
                holder.registerProblem(
                    comment,
                    MCDevBundle("fabric.annotater.javadoc.error.not_markdown"),
                    ProblemHighlightType.GENERIC_ERROR,
                    McdevReflection.CONSTRUCTOR_MARKDOWN_DOCUMENTATION_COMMENTS_MIGRATION_FIX.newInstanceUnwrap(),
                )
            }

            override fun visitComment(comment: PsiComment) {
                // Doc comments are visited by visitDocComment instead
                if (PsiTreeUtil.getParentOfType(comment, PsiDocComment::class.java) != null || isOriginal(comment)) {
                    return
                }
                holder.registerProblem(
                    comment,
                    MCDevBundle("fabric.annotater.javadoc.error.plain_comment"),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
    }
}
