/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.action.insertShadows
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findMatchingMethod
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.objectweb.asm.Opcodes

class MissingAbstractShadowsInEnumInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports missing abstract shadows in enum mixins"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            if (!aClass.isEnum || !aClass.isMixin) {
                return
            }

            val missingShadows = getMissingShadows(holder.project, aClass)

            if (missingShadows.isNotEmpty()) {
                holder.registerProblem(
                    aClass.nameIdentifier ?: aClass,
                    "Missing abstract shadows: ${missingShadows.joinToString { it.name }}",
                    AddMissingShadowsFix(aClass)
                )
            }
        }
    }

    private class AddMissingShadowsFix(aClass: PsiClass) : LocalQuickFixOnPsiElement(aClass) {
        override fun getFamilyName() = "Add missing shadows"
        override fun getText() = "Add missing shadows"

        override fun invoke(project: Project, psiFile: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val aClass = startElement as? PsiClass ?: return
            val missingShadows = getMissingShadows(project, aClass)
            insertShadows(project, aClass, missingShadows.asSequence())
        }
    }

    companion object {
        private fun getMissingShadows(project: Project, aClass: PsiClass): List<PsiMethod> {
            val requiredShadows = aClass.mixinTargets.flatMap { mixinTarget ->
                if (!mixinTarget.hasAccess(Opcodes.ACC_ABSTRACT)) {
                    return@flatMap emptyList()
                }
                mixinTarget.methods
                    .filter { it.hasAccess(Opcodes.ACC_ABSTRACT) }
                    .map { it.findOrConstructSourceMethod(mixinTarget, project, canDecompile = false) }
            }

            return requiredShadows.filter {
                aClass.findMatchingMethod(it, checkBases = false) == null
            }
        }
    }
}
