/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifierList

class MixinAnnotationsInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports Mixin annotations outside of @Mixin classes"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            val qualifiedName = annotation.qualifiedName ?: return
            if (MixinAnnotationHandler.forMixinAnnotation(qualifiedName, annotation.project) == null) {
                return
            }

            // Annotation must be either on or in a Mixin class
            val containingClass =
                (annotation.owner as? PsiModifierList)?.parent as? PsiClass ?: annotation.findContainingClass()
                    ?: return
            if (!containingClass.isMixin) {
                holder.registerProblem(
                    annotation,
                    "@${annotation.nameReferenceElement?.text} can be only used in a @Mixin class",
                    RemoveAnnotationQuickFix(annotation, null)
                )
            }
        }
    }
}
