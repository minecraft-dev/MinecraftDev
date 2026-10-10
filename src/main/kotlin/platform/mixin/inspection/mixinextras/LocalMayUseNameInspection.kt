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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifyVariableMayUseNameInspection.ReplaceWithNameFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifyVariableMayUseNameInspection.Util.getVariableNameToIntroduce
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.unwrapLocalRef
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandQuickFix
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.parentOfType

class LocalMayUseNameInspection : MixinInspection() {
    @JvmField
    var ignoreForImplicitLocals = false

    override fun getStaticDescription() = "Reports @Local relying on index or ordinal that may use a name instead"

    override fun getOptionsPane() = OptPane.pane(
        OptPane.checkbox("ignoreForImplicitLocals", "Ignore for implicit locals")
    )

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(localAnnotation: PsiAnnotation) {
            if (!localAnnotation.hasQualifiedName(MixinConstants.MixinExtras.LOCAL)) {
                return
            }

            val problemElement = localAnnotation.nameReferenceElement ?: return

            val parameter = localAnnotation.parentOfType<PsiParameter>() ?: return
            val method = parameter.declarationScope as? PsiMethod ?: return

            val localType = parameter.type.unwrapLocalRef()
            val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)

            if (ignoreForImplicitLocals && localInfo.isImplicit) {
                return
            }

            val (injector, injectorAnnotation) = method.annotations.mapFirstNotNull { annotation ->
                (MixinAnnotationHandler.forMixinAnnotation(annotation, holder.project) as? InsnInjectorAnnotationHandler)?.let { it to annotation }
            } ?: return

            val variableName = getVariableNameToIntroduce(localInfo, injector, injectorAnnotation) ?: return

            val fixes = mutableListOf<LocalQuickFix>(ReplaceWithNameFix(localAnnotation, variableName))

            if (localInfo.isImplicit) {
                fixes += IgnoreForImplicitLocalsFix()
            }

            holder.registerProblem(
                problemElement,
                "@Local can use variable name",
                *fixes.toTypedArray(),
            )
        }
    }

    private inner class IgnoreForImplicitLocalsFix : ModCommandQuickFix(), LowPriorityAction {
        override fun getFamilyName() = "Ignore for implicit locals"

        override fun perform(project: Project, descriptor: ProblemDescriptor): ModCommand {
            return ModCommand.updateInspectionOption(descriptor.psiElement, this@LocalMayUseNameInspection) {
                it.ignoreForImplicitLocals = true
            }
        }
    }
}
