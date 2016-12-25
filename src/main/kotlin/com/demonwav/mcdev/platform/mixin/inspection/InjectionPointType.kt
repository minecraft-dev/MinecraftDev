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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType

private val injectionPointAnnotations = InjectionPointType.values().associateBy { it.annotation }

internal fun findInjectionPointAnnotations(element: PsiAnnotationOwner): List<Pair<InjectionPointType, PsiAnnotation>> {
    return element.annotations.mapNotNull {
        val name = it.qualifiedName ?: return@mapNotNull null
        val type = injectionPointAnnotations[name] ?: return@mapNotNull null
        Pair(type, it)
    }
}

internal enum class InjectionPointType(val annotation: String) {
    INJECT(MixinConstants.Annotations.INJECT) {
        override fun isStrict(annotation: PsiAnnotation, targetMethod: PsiMethod): Boolean {
            return ((annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)?.referenceName ?: "NO_CAPTURE") == "NO_CAPTURE"
        }

        override fun expectedMethodParameters(annotation: PsiAnnotation, targetMethod: PsiMethod): List<Parameter> {
            val targetParameters = targetMethod.parameterList

            val result = ArrayList<Parameter>(targetParameters.parametersCount + 1)
            targetParameters.parameters.mapTo(result, {Parameter(it.name, it.type)})

            if (targetMethod.returnType == PsiType.VOID) {
                result.add(Parameter("ci", callbackInfoType(targetMethod.project)!!))
            } else {
                result.add(Parameter("cir", callbackInfoReturnableType(targetMethod.project, targetMethod.returnType!!)!!))
            }

            return result
        }
    },
    MODIFY_ARG(MixinConstants.Annotations.MODIFY_ARG),
    MODIFY_CONSTANT(MixinConstants.Annotations.MODIFY_CONSTANT),
    MODIFY_VARIABLE(MixinConstants.Annotations.MODIFY_VARIABLE),
    REDIRECT(MixinConstants.Annotations.REDIRECT);

    open fun isStrict(annotation: PsiAnnotation, targetMethod: PsiMethod) = true

    open fun expectedMethodParameters(annotation: PsiAnnotation, targetMethod: PsiMethod): List<Parameter>? = null

}
