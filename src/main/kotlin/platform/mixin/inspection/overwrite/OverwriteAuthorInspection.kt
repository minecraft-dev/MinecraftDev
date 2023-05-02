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

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment

class OverwriteAuthorInspection : OverwriteInspection() {

    override fun getStaticDescription() =
        "For maintainability reasons, the Sponge project requires @Overwrite methods to declare an @author JavaDoc tag."

    override fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation) {
        val javadoc = method.docComment
        if (javadoc == null) {
            registerMissingTags(holder, method)
            return
        }

        val authorTag = javadoc.findTagByName("author")
        if (authorTag == null) {
            registerMissingTag(holder, javadoc, "author")
        }

        val reasonTag = javadoc.findTagByName("reason")
        if (reasonTag == null) {
            registerMissingTag(holder, javadoc, "reason")
        }
    }

    private fun registerMissingTag(holder: ProblemsHolder, element: PsiElement, tag: String) {
        holder.registerProblem(
            element,
            "@Overwrite methods must have an associated JavaDoc with a filled in @$tag tag",
            QuickFix(tag),
        )
    }

    private fun registerMissingTags(holder: ProblemsHolder, element: PsiElement) {
        holder.registerProblem(
            element,
            "@Overwrite methods must have an associated JavaDoc with filled in @author and @reason tags",
            QuickFix(),
        )
    }

    private class QuickFix(val tag: String? = null) : LocalQuickFix {

        override fun getFamilyName() = "Add missing Javadoc tag"

        override fun getName(): String = if (tag == null) "Add all missing Javadoc tags" else "Add @$tag Javadoc tag"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val method = element as? PsiMethod ?: (element as PsiDocComment).owner as PsiMethod

            val javadoc = method.docComment
            if (javadoc == null) {
                // Create new Javadoc comment
                method.addBefore(
                    JavaPsiFacade.getElementFactory(project)
                        .createDocCommentFromText("/**\n * @author \n * @reason \n */"),
                    method.modifierList,
                )
                return
            }

            // Create new Javadoc tag
            val tag = JavaPsiFacade.getElementFactory(project).createDocTagFromText("@$tag")
            javadoc.addAfter(tag, null)
        }
    }
}
