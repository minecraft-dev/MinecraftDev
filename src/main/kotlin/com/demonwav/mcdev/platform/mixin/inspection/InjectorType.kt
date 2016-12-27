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
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiType

internal enum class InjectorType(val annotation: String) {
    INJECT(MixinConstants.Annotations.INJECT) {

        override fun expectedMethodParameters(annotation: PsiAnnotation, targetMethod: PsiMethod): List<ParameterGroup> {
            val targetParameters = targetMethod.parameterList
            val returnType = targetMethod.returnType

            val result = ArrayList<ParameterGroup>()

            // Parameters from injected method (optional)
            result.add(ParameterGroup(targetParameters.parameters.map(::Parameter), required = false, default = true))

            // Callback info (required)
            result.add(ParameterGroup(listOf(if (returnType == null || returnType == PsiType.VOID) {
                Parameter("ci", callbackInfoType(targetMethod.project)!!)
            } else {
                Parameter("cir", callbackInfoReturnableType(targetMethod.project,
                        if (returnType is PsiPrimitiveType) returnType.getBoxedType(targetMethod)!! else returnType)!!)
            })))

            // Captured locals (only if local capture is enabled)
            // Right now we allow any parameters here since we can't easily
            // detect the local variables that can be captured
            if (((annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
                    ?.referenceName ?: "NO_CAPTURE") != "NO_CAPTURE") {
                result.add(ParameterGroup(null))
            }

            return result
        }

    },
    MODIFY_ARG(MixinConstants.Annotations.MODIFY_ARG),
    MODIFY_CONSTANT(MixinConstants.Annotations.MODIFY_CONSTANT),
    MODIFY_VARIABLE(MixinConstants.Annotations.MODIFY_VARIABLE),
    REDIRECT(MixinConstants.Annotations.REDIRECT);

    val annotationName = "@${PsiNameHelper.getShortClassName(annotation)}"

    open fun expectedMethodParameters(annotation: PsiAnnotation, targetMethod: PsiMethod): List<ParameterGroup>? = null

    companion object {

        private val injectionPointAnnotations = InjectorType.values().associateBy { it.annotation }

        internal fun findAnnotations(element: PsiAnnotationOwner): List<Pair<InjectorType, PsiAnnotation>> {
            return element.annotations.mapNotNull {
                val name = it.qualifiedName ?: return@mapNotNull null
                val type = injectionPointAnnotations[name] ?: return@mapNotNull null
                Pair(type, it)
            }
        }

    }
}
