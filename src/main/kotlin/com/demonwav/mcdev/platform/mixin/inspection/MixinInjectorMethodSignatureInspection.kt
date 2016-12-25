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
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.elementsEqual
import com.demonwav.mcdev.util.startsWith
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

class MixinInjectorMethodSignatureInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getStaticDescription(): String? {
        return "Reports problems related to the method signature of Mixin injectors"
    }

    override fun checkMethod(method: PsiMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val identifier = method.nameIdentifier ?: return null
        val modifiers = method.modifierList

        val problems = arrayListOf<ProblemDescriptor>()

        for ((type, annotation) in findInjectionPointAnnotations(modifiers)) {
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
            val strict = type.isStrict(annotation, targetMethod)

            if (!checkParameters(parameters, expectedParameters, strict)) {
                problems.add(manager.createProblemDescriptor(parameters,
                        "Method parameters do not match expected parameters for ${type.annotationName}",
                        MixinInjectorUpdateMethodParametersQuickFix(expectedParameters), ProblemHighlightType.GENERIC_ERROR, isOnTheFly))
            }
        }

        return if (problems.isEmpty()) ProblemDescriptor.EMPTY_ARRAY else problems.toTypedArray()
    }

}

class MixinInjectorUpdateMethodParametersQuickFix(val expected: List<Parameter>) : LocalQuickFix {

    override fun getFamilyName(): String {
        return "Fix method parameters"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val parameters = descriptor.psiElement as PsiParameterList
        // TODO: Merge method parameters (keep existing parameter names)
        parameters.synchronize(expected.map { JavaPsiFacade.getElementFactory(project).createParameter(it.name ?: "", it.type) })
    }

}

private fun checkParameters(parameters: PsiParameterList, expected: List<Parameter>, strict: Boolean): Boolean {
    // Fail fast by checking if the count matches
    if (!checkParameterCount(parameters, expected, strict)) {
        return false
    }

    val currentParameters = parameters.parameters.asList()

    // Check if the types are equal
    return if (strict) {
        currentParameters.elementsEqual(expected, { c, (_, type) -> c.type == type })
    } else {
        currentParameters.startsWith(expected, { c, (_, type) -> c.type == type })
    }
}

private fun checkParameterCount(parameters: PsiParameterList, expected: List<Any>, strict: Boolean): Boolean {
    return if (strict) {
        parameters.parametersCount == expected.size
    } else {
        // Allow more parameters than expected
        parameters.parametersCount >= expected.size
    }
}
