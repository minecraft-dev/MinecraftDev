/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.FINAL
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MUTABLE
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression

class ShadowFinalInspection : MixinInspection() {

    override fun getStaticDescription() =
        "@Final annotated fields cannot be modified, as the field it is targeting is final. " +
            "This can be overridden with @Mutable."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
            val left = expression.lExpression as? PsiReferenceExpression ?: return
            val resolved = left.resolve() as? PsiModifierListOwner ?: return
            val modifiers = resolved.modifierList ?: return

            if (modifiers.findAnnotation(FINAL) != null && modifiers.findAnnotation(MUTABLE) == null) {
                holder.registerProblem(
                    expression, "@Final fields cannot be modified",
                    AddAnnotationFix(MUTABLE, resolved)
                )
            }
        }
    }
}
