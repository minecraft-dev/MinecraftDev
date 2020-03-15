/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
            registerMissingTag(holder, method)
            return
        }

        val tag = javadoc.findTagByName("author")
        if (tag == null) {
            registerMissingTag(holder, javadoc)
        }
    }

    private fun registerMissingTag(holder: ProblemsHolder, element: PsiElement) {
        holder.registerProblem(
            element,
            "@Overwrite methods must have an associated JavaDoc with a filled in @author tag",
            QuickFix
        )
    }

    private object QuickFix : LocalQuickFix {

        override fun getFamilyName() = "Add @author Javadoc tag"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val method = element as? PsiMethod ?: (element as PsiDocComment).owner as PsiMethod

            val javadoc = method.docComment
            if (javadoc == null) {
                // Create new Javadoc comment
                method.addBefore(
                    JavaPsiFacade.getElementFactory(project).createDocCommentFromText("/**\n * @author \n */"),
                    method.modifierList
                )
                return
            }

            // Create new Javadoc tag
            val tag = JavaPsiFacade.getElementFactory(project).createDocTagFromText("@author")
            javadoc.addAfter(tag, null)
        }
    }
}
