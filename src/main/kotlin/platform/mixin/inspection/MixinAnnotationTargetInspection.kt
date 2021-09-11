/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType

class MixinAnnotationTargetInspection : MixinInspection() {
    override fun getStaticDescription() = "Verifies targets of mixin annotations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitField(field: PsiField) {
                super.visitField(field)
                processAnnotations(field.annotations)
            }

            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                processAnnotations(method.annotations)
            }

            private fun processAnnotations(annotations: Array<PsiAnnotation>) {
                for (annotation in annotations) {
                    val qName = annotation.qualifiedName ?: continue
                    val handler = MixinAnnotationHandler.forMixinAnnotation(qName) ?: continue
                    val unresolvedClasses = handler.getUnresolvedClasses(annotation)
                    if (unresolvedClasses.isNotEmpty()) {
                        val unresolvedClassesStr = unresolvedClasses.joinToString(", ") { it.substringAfterLast('/') }
                        val message = handler.createUnresolvedMessage(annotation, unresolvedClassesStr) ?: continue
                        val quickFix = RemoveAnnotationQuickFix(annotation, annotation.parentOfType())
                        holder.registerProblem(annotation, message, quickFix)
                    }
                }
            }
        }
    }
}
