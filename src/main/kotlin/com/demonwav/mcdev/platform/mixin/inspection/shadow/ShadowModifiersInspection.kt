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
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.FINAL
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.resolveFirstShadowTarget
import com.demonwav.mcdev.util.findKeyword
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.PsiUtil.ACCESS_LEVEL_PRIVATE
import com.intellij.psi.util.PsiUtil.ACCESS_LEVEL_PROTECTED

class ShadowModifiersInspection : MixinInspection() {

    override fun getStaticDescription() = "Validates access modifiers of @Shadow members"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (annotation.qualifiedName != MixinConstants.Annotations.SHADOW) {
                return
            }

            val shadowModifierList = annotation.owner as? PsiModifierList ?: return
            val member = shadowModifierList.parent as? PsiMember ?: return
            val psiClass = member.containingClass ?: return
            val targetClasses = psiClass.mixinTargets.ifEmpty { return }
            val target = resolveFirstShadowTarget(annotation, targetClasses, member)?.modifierList ?: return

            // Check static modifier
            val targetStatic = target.hasModifierProperty(PsiModifier.STATIC)
            if (targetStatic != shadowModifierList.hasModifierProperty(PsiModifier.STATIC)) {
                val message = if (targetStatic) {
                    "@Shadow for static method should be also static"
                } else {
                    "@Shadow target method is not static"
                }

                holder.registerProblem(
                    shadowModifierList.findKeyword(PsiModifier.STATIC) ?: annotation, message,
                    ProblemHighlightType.GENERIC_ERROR,
                    QuickFixFactory.getInstance().createModifierListFix(
                        shadowModifierList,
                        PsiModifier.STATIC,
                        targetStatic,
                        false
                    )
                )
            }

            // Check access level
            val targetAccessLevel = getTargetAccessLevel(target, shadowModifierList)
            val shadowAccessLevel = PsiUtil.getAccessLevel(shadowModifierList)
            if (targetAccessLevel != shadowAccessLevel) {
                val targetModifier = PsiUtil.getAccessModifier(targetAccessLevel)
                val shadowModifier = PsiUtil.getAccessModifier(shadowAccessLevel)
                holder.registerProblem(
                    shadowModifierList.findKeyword(shadowModifier) ?: annotation,
                    "Invalid access modifiers, has: $shadowModifier, but target member has: " +
                        PsiUtil.getAccessModifier(PsiUtil.getAccessLevel(target)),
                    QuickFixFactory.getInstance().createModifierListFix(shadowModifierList, targetModifier, true, false)
                )
            }

            // TODO: Would it make sense to apply the @Final check to methods?
            if (member !is PsiField) {
                return
            }

            // Check @Final
            val targetFinal = target.hasModifierProperty(PsiModifier.FINAL)
            val shadowFinal = shadowModifierList.findAnnotation(FINAL)
            if (targetFinal != (shadowFinal != null)) {
                if (targetFinal) {
                    holder.registerProblem(
                        annotation, "@Shadow for final member should be annotated as @Final",
                        AddAnnotationFix(FINAL, member)
                    )
                } else {
                    holder.registerProblem(
                        shadowFinal!!, "Target method is not final",
                        RemoveAnnotationQuickFix(shadowFinal, member)
                    )
                }
            }
        }

        private fun getTargetAccessLevel(target: PsiModifierList, shadow: PsiModifierList): Int {
            val targetAccessLevel = PsiUtil.getAccessLevel(target)

            // Abstract @Shadow methods for private methods are represented using protected
            return if (targetAccessLevel == ACCESS_LEVEL_PRIVATE && shadow.hasModifierProperty(PsiModifier.ABSTRACT)) {
                ACCESS_LEVEL_PROTECTED
            } else {
                targetAccessLevel
            }
        }
    }
}
