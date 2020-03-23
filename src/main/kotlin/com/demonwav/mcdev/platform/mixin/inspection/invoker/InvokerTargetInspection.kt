/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.invoker

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveInvokerTarget
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierList

class InvokerTargetInspection : MixinInspection() {

    override fun getStaticDescription() = "Validates targets of @Invoker members"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (annotation.qualifiedName != MixinConstants.Annotations.INVOKER) {
                return
            }

            val modifierList = annotation.owner as? PsiModifierList ?: return
            val member = modifierList.parent as? PsiMember ?: return
            val psiClass = member.containingClass ?: return
            val targetClasses = psiClass.mixinTargets.ifEmpty { return }

            if (resolveInvokerTarget(annotation, targetClasses, member) == null) {
                val value = annotation.findDeclaredAttributeValue("value")?.constantStringValue

                holder.registerProblem(
                        annotation, "Cannot resolve member '${value}' in target class",
                        RemoveAnnotationQuickFix(annotation, member)
                )
            }
        }
    }
}