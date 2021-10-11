/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.util.TypeConversionUtil

data class ParameterGroup(
    val parameters: List<Parameter>?,
    val required: Boolean = parameters != null,
    val default: Boolean = required
) {

    val size
        get() = this.parameters?.size ?: 0

    fun match(parameters: Array<PsiParameter>, currentPosition: Int, allowCoerce: Boolean): Boolean {
        if (this.parameters == null) {
            // Wildcard parameter groups always match
            return true
        }

        // Check if remaining parameter count is enough
        if (currentPosition + size > parameters.size) {
            return false
        }

        var pos = currentPosition

        // Check parameter types
        for ((_, expectedType) in this.parameters) {
            val parameter = parameters[pos++]
            if (!matchParameter(expectedType, parameter, allowCoerce)) {
                return false
            }
        }

        return true
    }

    companion object {
        private val INT_TYPES = setOf(PsiType.INT, PsiType.SHORT, PsiType.CHAR, PsiType.BYTE, PsiType.BOOLEAN)

        private fun matchParameter(expectedType: PsiType, parameter: PsiParameter, allowCoerce: Boolean): Boolean {
            val normalizedExpected = normalizeType(expectedType)
            val normalizedParameter = normalizeType(parameter.type)
            if (normalizedExpected == normalizedParameter) {
                return true
            }
            if (!allowCoerce || !parameter.hasAnnotation(COERCE)) {
                return false
            }

            if (normalizedExpected is PsiPrimitiveType) {
                if (normalizedParameter !is PsiPrimitiveType) {
                    return false
                }
                return normalizedExpected in INT_TYPES && normalizedParameter in INT_TYPES
            }
            return isAssignable(normalizedParameter, normalizedExpected)
        }

        private fun normalizeType(type: PsiType): PsiType {
            val erasure = TypeConversionUtil.erasure(type)
            return if (erasure is PsiEllipsisType) {
                erasure.toArrayType()
            } else {
                erasure
            }
        }
    }
}
