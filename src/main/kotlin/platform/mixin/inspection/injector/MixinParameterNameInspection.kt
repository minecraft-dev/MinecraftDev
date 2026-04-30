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
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.hasNamedLocalVariables
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.options.OptPane
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandQuickFix
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.siyeh.ig.fixes.RenameFix

class MixinParameterNameInspection : MixinInspection() {
    @JvmField
    var reportForMainSignature = true
    @JvmField
    var reportForLocal = true

    override fun getStaticDescription() = "Reports when a parameter name in a Mixin does not match the target class"

    override fun getOptionsPane() = OptPane.pane(
        OptPane.checkbox("reportForMainSignature", "Report for main signature"),
        OptPane.checkbox("reportForLocal", "Report for @Local parameters")
    )

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val module = method.findModule() ?: return
            val parameters = method.parameterList.parameters
            val parametersWithoutSugar = parameters.dropLastWhile { it.isMixinExtrasSugar }.toTypedArray()

            val validNames = arrayOfNulls<MutableSet<String>>(parameters.size)

            for (annotation in method.annotations) {
                val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, holder.project)
                    as? InjectorAnnotationHandler ?: continue
                for (target in MixinAnnotationHandler.resolveTarget(annotation)) {
                    if (!collectValidNames(validNames, module, method, parameters, parametersWithoutSugar, annotation, handler, target)) {
                        return
                    }
                }
            }

            for (index in validNames.indices) {
                val names = validNames[index] ?: continue
                if (names.isEmpty()) {
                    continue
                }

                if (parameters[index].name !in names) {
                    val fixes = mutableListOf<LocalQuickFix>()

                    for (name in names) {
                        fixes += RenameFix(name, false, false)
                    }

                    fixes += if (index < parametersWithoutSugar.size) {
                        DontReportForMainSignatureFix()
                    } else {
                        DontReportForLocalFix()
                    }

                    holder.registerProblem(
                        parameters[index].nameIdentifier ?: parameters[index],
                        "Parameter name does not match name ${names.joinToString(" / ") { "'$it'" } } in target class",
                        *fixes.toTypedArray()
                    )
                }
            }
        }
    }

    private fun collectValidNames(
        validNames: Array<MutableSet<String>?>,
        module: Module, method: PsiMethod,
        parameters: Array<PsiParameter>,
        parametersWithoutSugar: Array<PsiParameter>,
        annotation: PsiAnnotation,
        handler: InjectorAnnotationHandler,
        target: MixinTargetMember
    ): Boolean {
        if (target !is MethodTargetMember) {
            return true
        }

        if (!method.hasNamedLocalVariables(target.classAndMethod.clazz.name.replace('/', '.'))) {
            return true
        }

        val validNamesForThisTarget = arrayOfNulls<MutableSet<String>>(parameters.size)

        if (reportForMainSignature) {
            val expectedSignatures =
                handler.expectedMethodSignature(
                    annotation,
                    target.classAndMethod.clazz,
                    target.classAndMethod.method,
                    (target as? ContextAwareMethodTargetMember)?.selector
                )
                    ?: return false
            var anyValidSignatures = false

            for ((expectedParams, _) in expectedSignatures) {
                if (InvalidInjectorMethodSignatureInspection.Util.checkParameters(
                        method.parameterList,
                        expectedParams,
                        handler.allowCoerce
                    ) != InvalidInjectorMethodSignatureInspection.CheckResult.OK
                ) {
                    continue
                }

                anyValidSignatures = true

                checkExpectedSignatureForKnownNames(
                    validNamesForThisTarget,
                    handler,
                    expectedParams,
                    parametersWithoutSugar
                )
            }

            if (!anyValidSignatures) {
                return false
            }
        }

        if (reportForLocal) {
            for (pos in parametersWithoutSugar.size until parameters.size) {
                if (!checkSugarForKnownNames(
                        validNamesForThisTarget,
                        module,
                        handler,
                        annotation,
                        target,
                        parameters[pos],
                        pos
                    )
                ) {
                    return false
                }
            }
        }

        for (index in validNamesForThisTarget.indices) {
            val names = validNamesForThisTarget[index] ?: continue
            if (validNames[index] == null) {
                validNames[index] = names
            } else {
                validNames[index]!!.retainAll(names)
            }
        }

        return true
    }

    private fun checkExpectedSignatureForKnownNames(
        validNamesForThisTarget: Array<MutableSet<String>?>,
        handler: InjectorAnnotationHandler,
        expectedParams: List<ParameterGroup>,
        parametersWithoutSugar: Array<PsiParameter>
    ) {
        if (parametersWithoutSugar.isEmpty()) {
            return
        }

        var pos = 0
        for (group in expectedParams) {
            if (!group.match(parametersWithoutSugar, pos, handler.allowCoerce)) {
                continue
            }
            for (expectedParam in group.parameters) {
                if (expectedParam.knownName && expectedParam.name != null) {
                    if (validNamesForThisTarget[pos] == null) {
                        validNamesForThisTarget[pos] = mutableSetOf(expectedParam.name)
                    } else {
                        validNamesForThisTarget[pos]!!.add(expectedParam.name)
                    }
                }
                pos++
                if (pos >= parametersWithoutSugar.size) {
                    return
                }
            }
        }
    }

    private fun checkSugarForKnownNames(
        validNamesForThisTarget: Array<MutableSet<String>?>,
        module: Module,
        handler: InjectorAnnotationHandler,
        annotation: PsiAnnotation,
        target: MethodTargetMember,
        parameter: PsiParameter,
        pos: Int
    ): Boolean {
        val localAnnotation = parameter.getAnnotation(MixinConstants.MixinExtras.LOCAL) ?: return true
        val localInfo = LocalInfo.fromAnnotation(parameter.type, localAnnotation)

        for (insn in handler.resolveInstructions(
            annotation,
            target.classAndMethod.clazz,
            target.classAndMethod.method,
            (target as? ContextAwareMethodTargetMember)?.selector
        )) {
            val matchedLocal = localInfo.matchLocals(
                module,
                target.classAndMethod.clazz,
                target.classAndMethod.method,
                insn.insn, CollectVisitor.Mode.RESOLUTION
            )?.singleOrNull() ?: return false
            if (matchedLocal.isNamed) {
                if (validNamesForThisTarget[pos] == null) {
                    validNamesForThisTarget[pos] = mutableSetOf(matchedLocal.name)
                } else {
                    validNamesForThisTarget[pos]!!.add(matchedLocal.name)
                }
            }
        }

        return true
    }

    private inner class DontReportForMainSignatureFix : ModCommandQuickFix(), LowPriorityAction {
        override fun getFamilyName() = "Don't report for main signature"

        override fun perform(project: Project, descriptor: ProblemDescriptor): ModCommand {
            return ModCommand.updateInspectionOption(descriptor.psiElement, this@MixinParameterNameInspection) {
                it.reportForMainSignature = false
            }
        }
    }

    private inner class DontReportForLocalFix : ModCommandQuickFix(), LowPriorityAction {
        override fun getFamilyName() = "Don't report for @Local parameters"

        override fun perform(project: Project, descriptor: ProblemDescriptor): ModCommand {
            return ModCommand.updateInspectionOption(descriptor.psiElement, this@MixinParameterNameInspection) {
                it.reportForLocal = false
            }
        }
    }
}
