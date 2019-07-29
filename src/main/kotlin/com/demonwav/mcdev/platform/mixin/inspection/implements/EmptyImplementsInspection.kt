/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.implements

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.util.findAnnotations
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor

class EmptyImplementsInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports empty @Implements annotations (without an @Interface)"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (annotation.qualifiedName != IMPLEMENTS) {
                return
            }

            val interfaces = annotation.findDeclaredAttributeValue(null)?.findAnnotations()
            if (interfaces == null || interfaces.isEmpty()) {
                holder.registerProblem(
                    annotation, "@Implements is redundant",
                    RemoveAnnotationQuickFix(annotation, null)
                )
            }
        }
    }
}
