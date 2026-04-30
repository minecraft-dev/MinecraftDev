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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.hasNamedLocalVariables
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findModule
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

class ModifyVariableMayUseNameInspection : MixinInspection() {
    @JvmField
    var ignoreForImplicitLocals = false

    override fun getStaticDescription() = "Reports @ModifyVariable injectors relying on index or ordinal that may use a name instead"

    override fun getOptionsPane() = OptPane.pane(
        OptPane.checkbox("ignoreForImplicitLocals", "Ignore for implicit locals")
    )

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val modifyVariable = method.findAnnotation(MODIFY_VARIABLE) ?: return
            val localType = method.parameterList.getParameter(0)?.type ?: return
            val problemElement = modifyVariable.nameReferenceElement ?: return

            val injector =
                MixinAnnotationHandler.forMixinAnnotation(MODIFY_VARIABLE) as? InjectorAnnotationHandler ?: return
            val localInfo = LocalInfo.fromAnnotation(localType, modifyVariable)

            if (ignoreForImplicitLocals && localInfo.isImplicit) {
                return
            }

            val variableName = Util.getVariableNameToIntroduce(localInfo, injector, modifyVariable) ?: return

            val fixes = mutableListOf<LocalQuickFix>(ReplaceWithNameFix(modifyVariable, variableName))

            if (localInfo.isImplicit) {
                fixes += IgnoreForImplicitLocalsFix()
            }

            holder.registerProblem(
                problemElement,
                "@ModifyVariable can use variable name",
                *fixes.toTypedArray(),
            )
        }
    }

    class ReplaceWithNameFix(
        annotation: PsiAnnotation,
        private val variableName: String,
    ) : AnnotationAttributeFix(annotation, "name" to variableName, "ordinal" to null, "index" to null) {
        override fun getText() = "Use variable name '$variableName'"
        override fun getFamilyName() = "Use variable name '$variableName'"
    }

    private inner class IgnoreForImplicitLocalsFix : ModCommandQuickFix(), LowPriorityAction {
        override fun getFamilyName() = "Ignore for implicit locals"

        override fun perform(project: Project, descriptor: ProblemDescriptor): ModCommand {
            return ModCommand.updateInspectionOption(descriptor.psiElement, this@ModifyVariableMayUseNameInspection) {
                it.ignoreForImplicitLocals = true
            }
        }
    }

    object Util {
        fun getVariableNameToIntroduce(
            localInfo: LocalInfo,
            injector: InjectorAnnotationHandler,
            injectorAnnotation: PsiAnnotation,
        ): String? {
            if (localInfo.index == null && localInfo.ordinal == null && localInfo.names.isNotEmpty()) {
                // already a named local
                return null
            }

            val module = injectorAnnotation.findModule() ?: return null
            val mixinTargets = injectorAnnotation.findContainingClass()?.mixinTargets ?: return null

            var variableName: String? = null
            for (targetClass in mixinTargets) {
                if (!injectorAnnotation.hasNamedLocalVariables(targetClass.name.replace('/', '.'))) {
                    return null
                }

                for (targetMember in injector.resolveTarget(injectorAnnotation, targetClass)) {
                    val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: continue
                    for (insn in injector.resolveInstructions(
                        injectorAnnotation,
                        targetMethod.clazz,
                        targetMethod.method,
                        (targetMember as? ContextAwareMethodTargetMember)?.selector
                    )) {
                        val matchedLocals = localInfo.matchLocals(
                            module,
                            targetMethod.clazz,
                            targetMethod.method,
                            insn.insn,
                            CollectVisitor.Mode.RESOLUTION
                        ) ?: return null

                        for (local in matchedLocals) {
                            if (!local.isNamed) {
                                return null
                            }
                            if (variableName != null && local.name != variableName) {
                                return null
                            }
                            variableName = local.name
                        }
                    }
                }
            }

            return variableName
        }
    }
}
