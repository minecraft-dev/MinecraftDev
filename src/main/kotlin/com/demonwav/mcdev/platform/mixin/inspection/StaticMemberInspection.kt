/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier

class StaticMemberInspection : MixinInspection() {

    override fun getStaticDescription() =
        "A mixin class does not exist at runtime, and thus having them public does not make sense. " +
            "Make the field/method private instead."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            visitMember(method)
        }

        override fun visitField(field: PsiField) {
            visitMember(field)
        }

        private fun visitMember(member: PsiMember) {
            if (isProblematic(member)) {
                holder.registerProblem(
                    member, "Public static members are not allowed in Mixin classes",
                    QuickFixFactory.getInstance().createModifierListFix(member, PsiModifier.PRIVATE, true, false)
                )
            }
        }

        private fun isProblematic(member: PsiMember): Boolean {
            val containingClass = member.containingClass ?: return false
            if (!containingClass.isMixin) {
                return false
            }

            val modifiers = member.modifierList!!

            return modifiers.hasModifierProperty(PsiModifier.PUBLIC) &&
                modifiers.hasModifierProperty(PsiModifier.STATIC) &&
                modifiers.findAnnotation(SHADOW) == null &&
                modifiers.findAnnotation(OVERWRITE) == null &&
                modifiers.findAnnotation(ACCESSOR) == null &&
                modifiers.findAnnotation(INVOKER) == null
        }
    }
}
