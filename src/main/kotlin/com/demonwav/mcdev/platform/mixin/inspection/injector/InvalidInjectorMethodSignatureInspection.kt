/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.demonwav.mcdev.util.synchronize
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameterList
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind

class InvalidInjectorMethodSignatureInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports problems related to the method signature of Mixin injectors"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            val identifier = method.nameIdentifier ?: return
            val modifiers = method.modifierList

            var reportedStatic = false
            var reportedSignature = false

            for ((type, annotation) in InjectorType.findAnnotations(modifiers)) {
                val methodAttribute = annotation.findDeclaredAttributeValue("method") ?: continue
                val targetMethods = MethodReference.resolveAllIfNotAmbiguous(methodAttribute) ?: continue

                for (targetMethod in targetMethods) {
                    if (!reportedStatic) {
                        val static = targetMethod.hasModifierProperty(PsiModifier.STATIC)
                        if (static && !modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                            reportedStatic = true
                            holder.registerProblem(
                                identifier, "Method must be static",
                                QuickFixFactory.getInstance().createModifierListFix(
                                    modifiers,
                                    PsiModifier.STATIC,
                                    true,
                                    false
                                )
                            )
                        }
                    }

                    if (!reportedSignature) {
                        // Check method parameters
                        val parameters = method.parameterList
                        val (expectedParameters, expectedReturnType) = type.expectedMethodSignature(
                            annotation,
                            targetMethod
                        ) ?: continue

                        if (!checkParameters(parameters, expectedParameters)) {
                            reportedSignature = true

                            holder.registerProblem(
                                parameters,
                                "Method parameters do not match expected parameters for ${type.annotationName}",
                                createMethodParametersFix(parameters, expectedParameters)
                            )
                        }

                        val methodReturnType = method.returnType
                        if (methodReturnType == null || !methodReturnType.isErasureEquivalentTo(expectedReturnType)) {
                            reportedSignature = true

                            holder.registerProblem(
                                method.returnTypeElement ?: identifier,
                                "Expected return type '${expectedReturnType.presentableText}' " +
                                    "for ${type.annotationName} method",
                                QuickFixFactory.getInstance().createMethodReturnFix(method, expectedReturnType, false)
                            )
                        }
                    }
                }
            }
        }

        private fun createMethodParametersFix(
            parameters: PsiParameterList,
            expected: List<ParameterGroup>
        ): LocalQuickFix? {
            // TODO: Someone should improve this: Right now we can only automatically fix if the parameters are empty
            return if (parameters.parametersCount == 0) ParametersQuickFix(expected) else null
        }

        private fun checkParameters(parameterList: PsiParameterList, expected: List<ParameterGroup>): Boolean {
            val parameters = parameterList.parameters
            var pos = 0

            for (group in expected) {
                // Check if parameter group matches
                if (group.match(parameters, pos)) {
                    pos += group.size
                } else if (group.required) {
                    return false
                }
            }

            return true
        }
    }

    private class ParametersQuickFix(private val expected: List<ParameterGroup>) : LocalQuickFix {

        override fun getFamilyName() = "Fix method parameters"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val parameters = descriptor.psiElement as PsiParameterList
            parameters.synchronize(expected.flatMap {
                if (it.default) {
                    it.parameters?.map {
                        JavaPsiFacade.getElementFactory(project).createParameter(
                            it.name ?: JavaCodeStyleManager.getInstance(project)
                                .suggestVariableName(VariableKind.PARAMETER, null, null, it.type).names
                                .firstOrNull() ?: "unknown", it.type
                        )
                    } ?: listOf()
                } else {
                    listOf()
                }
            })
        }
    }
}
