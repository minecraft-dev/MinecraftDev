/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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
        "A mixin class does not exist at runtime, and thus having them not private does not make sense. " +
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
                    member,
                    "Non-private static members are not allowed in Mixin classes",
                    QuickFixFactory.getInstance().createModifierListFix(member, PsiModifier.PRIVATE, true, false),
                )
            }
        }

        private fun isProblematic(member: PsiMember): Boolean {
            val containingClass = member.containingClass ?: return false
            if (!containingClass.isMixin) {
                return false
            }

            val modifiers = member.modifierList!!

            return !modifiers.hasModifierProperty(PsiModifier.PRIVATE) &&
                modifiers.hasModifierProperty(PsiModifier.STATIC) &&
                modifiers.findAnnotation(SHADOW) == null &&
                modifiers.findAnnotation(OVERWRITE) == null &&
                modifiers.findAnnotation(ACCESSOR) == null &&
                modifiers.findAnnotation(INVOKER) == null
        }
    }
}
