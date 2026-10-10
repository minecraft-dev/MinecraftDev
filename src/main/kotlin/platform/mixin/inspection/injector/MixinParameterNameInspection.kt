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
import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.hasNamedLocalVariables
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.ifNullOrEmpty
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
            val parametersWithoutSugar = parameters.dropLastWhile { it.isMixinExtrasSugar }

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
                val names = validNames[index].ifNullOrEmpty { continue }

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
        parametersWithoutSugar: List<PsiParameter>,
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

        if (reportForMainSignature) {
            val expectedSignatures = handler.expectedMethodSignatures(annotation, listOf(target.classAndMethod))

            for (expected in expectedSignatures) {
                val signatures = when (expected) {
                    ExpectedSignatures.Unknown -> continue
                    ExpectedSignatures.Invalid -> return false
                    is ExpectedSignatures.Valid -> expected.expected
                }
                val validNamesForThisTarget = Array<MutableSet<String>?>(parameters.size) { mutableSetOf() }
                var anyValidSignatures = false

                for (expectedSignature in signatures.options) {
                    if (!expectedSignature.matches(method)) {
                        continue
                    }
                    anyValidSignatures = true

                    checkExpectedSignatureForKnownNames(
                        validNamesForThisTarget,
                        expectedSignature.requiredParams + expectedSignature.trailingParams,
                        parametersWithoutSugar,
                    )
                }

                if (!anyValidSignatures) {
                    return false
                }

                for ((pos, names) in validNamesForThisTarget.withIndex()) {
                    val suggestions = names ?: mutableSetOf()
                    val existingNames = validNames[pos]
                    if (existingNames == null) {
                        validNames[pos] = suggestions
                    } else {
                        existingNames.retainAll(suggestions)
                    }
                }
            }
        }

        if (reportForLocal) {
            for (pos in parametersWithoutSugar.size until parameters.size) {
                if (!checkSugarForKnownNames(
                        validNames,
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

        return true
    }

    private fun checkExpectedSignatureForKnownNames(
        validNamesForThisTarget: Array<MutableSet<String>?>,
        expectedParams: List<Parameter>,
        parametersWithoutSugar: List<PsiParameter>
    ) {
        if (parametersWithoutSugar.isEmpty()) {
            return
        }

        for (pos in parametersWithoutSugar.indices) {
            val expectedParam = expectedParams[pos]
            if (!expectedParam.knownName || expectedParam.name == null) {
                validNamesForThisTarget[pos] = null
            } else {
                validNamesForThisTarget[pos]?.add(expectedParam.name)
            }
        }
    }

    private fun checkSugarForKnownNames(
        validNames: Array<MutableSet<String>?>,
        module: Module,
        handler: InjectorAnnotationHandler,
        annotation: PsiAnnotation,
        target: MethodTargetMember,
        parameter: PsiParameter,
        pos: Int
    ): Boolean {
        if (handler !is InsnInjectorAnnotationHandler) {
            // Can't have locals
            return true
        }
        val localAnnotation = parameter.getAnnotation(MixinConstants.MixinExtras.LOCAL) ?: return true
        val localInfo = LocalInfo.fromAnnotation(parameter.type, localAnnotation)

        for (insn in handler.resolveInstructions(
            annotation,
            target.classAndMethod.clazz,
            target.classAndMethod.method
        )) {
            val matchedLocal = localInfo.matchLocals(
                module,
                target.classAndMethod.clazz,
                target.classAndMethod.method,
                insn.insn, CollectVisitor.Mode.RESOLUTION
            )?.singleOrNull() ?: return false
            val suggestions = if (matchedLocal.isNamed) mutableSetOf(matchedLocal.name) else mutableSetOf()

            val existingNames = validNames[pos]
            if (existingNames == null) {
                validNames[pos] = suggestions
            } else {
                existingNames.retainAll(suggestions)
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
