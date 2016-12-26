/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.reference.createMethodReference
import com.demonwav.mcdev.util.synchronize
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameterList
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.util.containers.nullize

class MixinInjectorMethodSignatureInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getStaticDescription() = "Reports problems related to the method signature of Mixin injectors"

    override fun checkMethod(method: PsiMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val identifier = method.nameIdentifier ?: return null
        val modifiers = method.modifierList

        val problems = arrayListOf<ProblemDescriptor>()

        for ((type, annotation) in InjectionPointType.findAnnotations(modifiers)) {
            val methodAttribute = annotation.findDeclaredAttributeValue("method") as? PsiLiteral ?: continue
            val targetMethod = createMethodReference(methodAttribute)?.resolveFirstIfValid() as? PsiMethod ?: continue

            val static = targetMethod.hasModifierProperty(PsiModifier.STATIC)
            if (static != modifiers.hasModifierProperty(PsiModifier.STATIC)) {
                problems.add(manager.createProblemDescriptor(identifier,
                        if (static) "Method should be static" else "Method should not be static",
                        QuickFixFactory.getInstance().createModifierListFix(modifiers, PsiModifier.STATIC, static, false),
                        ProblemHighlightType.GENERIC_ERROR, isOnTheFly))
            }

            // Check method parameters
            val parameters = method.parameterList

            val expectedParameters = type.expectedMethodParameters(annotation, targetMethod) ?: continue

            if (!checkParameters(parameters, expectedParameters)) {
                problems.add(manager.createProblemDescriptor(parameters,
                        "Method parameters do not match expected parameters for ${type.annotationName}",
                        createMethodParametersFix(parameters, expectedParameters), ProblemHighlightType.GENERIC_ERROR, isOnTheFly))
            }
        }

        return problems.nullize()?.toTypedArray()
    }

}

internal fun createMethodParametersFix(parameters: PsiParameterList, expected: List<ParameterGroup>): LocalQuickFix? {
    // TODO: Someone should improve this: Right now we can only automatically fix if the parameters are empty
    return if (parameters.parametersCount == 0) MixinInjectorUpdateMethodParametersQuickFix(expected) else null
}

private class MixinInjectorUpdateMethodParametersQuickFix(val expected: List<ParameterGroup>) : LocalQuickFix {

    override fun getFamilyName() = "Fix method parameters"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val parameters = descriptor.psiElement as PsiParameterList
        parameters.synchronize(expected.flatMap {
            if (it.default) {
                it.parameters?.map { JavaPsiFacade.getElementFactory(project).createParameter(
                        it.name ?: JavaCodeStyleManager.getInstance(project)
                        .suggestVariableName(VariableKind.PARAMETER, null, null, it.type).names
                                .firstOrNull() ?: "unknown", it.type) } ?: listOf()
            } else {
                listOf()
            }
        })
    }

}

internal fun checkParameters(parameterList: PsiParameterList, expected: List<ParameterGroup>): Boolean {
    val parameters = parameterList.parameters
    var pos = 0

    for (group in expected) {
        // No parameters left in current method signature
        if (pos >= parameters.size) {
            // If the group is required some method parameters are missing
            if (group.required) {
                return false
            }

            // Continue to check for required parameter groups
            continue
        }

        // Check if parameter group matches
        if (group.match(parameters, pos)) {
            pos += group.size
        } else if (group.required) {
            return false
        }
    }

    return pos >= parameters.size || expected.last().wildcard
}
