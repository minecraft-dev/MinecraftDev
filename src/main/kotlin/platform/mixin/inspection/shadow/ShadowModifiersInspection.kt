/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.FINAL
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.accessLevel
import com.demonwav.mcdev.util.findKeyword
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
import org.objectweb.asm.Opcodes

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
            val target = MixinAnnotationHandler.resolveTarget(annotation).firstOrNull() ?: return

            // Check static modifier
            val targetStatic = (target.access and Opcodes.ACC_STATIC) != 0
            if (targetStatic != shadowModifierList.hasModifierProperty(PsiModifier.STATIC)) {
                val message = if (targetStatic) {
                    "@Shadow for static method should be also static"
                } else {
                    "@Shadow target method is not static"
                }

                holder.registerProblem(
                    shadowModifierList.findKeyword(PsiModifier.STATIC) ?: annotation,
                    message,
                    ProblemHighlightType.GENERIC_ERROR,
                    QuickFixFactory.getInstance().createModifierListFix(
                        shadowModifierList,
                        PsiModifier.STATIC,
                        targetStatic,
                        false,
                    ),
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
                        PsiUtil.getAccessModifier(targetAccessLevel),
                    QuickFixFactory.getInstance()
                        .createModifierListFix(shadowModifierList, targetModifier, true, false)
                )
            }

            // TODO: Would it make sense to apply the @Final check to methods?
            if (member !is PsiField) {
                return
            }

            // @Final annotation doesn't apply to members that are initialized in the mixin class
            if (member.hasInitializer()) {
                return
            }

            // Check @Final
            val targetFinal = (target.access and Opcodes.ACC_FINAL) != 0
            val shadowFinal = shadowModifierList.findAnnotation(FINAL)
            if (targetFinal != (shadowFinal != null)) {
                if (targetFinal) {
                    holder.registerProblem(
                        annotation,
                        "@Shadow for final member should be annotated as @Final",
                        AddAnnotationFix(FINAL, member),
                    )
                } else {
                    holder.registerProblem(
                        shadowFinal!!,
                        "Target method is not final",
                        RemoveAnnotationQuickFix(shadowFinal, member),
                    )
                }
            }
        }

        private fun getTargetAccessLevel(target: MixinTargetMember, shadow: PsiModifierList): Int {
            val targetAccessLevel = when (target) {
                is FieldTargetMember -> target.classAndField.field.accessLevel
                is MethodTargetMember -> target.classAndMethod.method.accessLevel
            }

            // Abstract @Shadow methods for private methods are represented using protected
            return if (targetAccessLevel == ACCESS_LEVEL_PRIVATE && shadow.hasModifierProperty(PsiModifier.ABSTRACT)) {
                ACCESS_LEVEL_PROTECTED
            } else {
                targetAccessLevel
            }
        }
    }
}
