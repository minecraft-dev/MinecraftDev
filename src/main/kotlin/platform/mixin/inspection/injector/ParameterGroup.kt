/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.normalize
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType

data class ParameterGroup(
    val parameters: List<Parameter>,
    val required: RequiredLevel = RequiredLevel.ERROR_IF_ABSENT,
    val default: Boolean = required != RequiredLevel.OPTIONAL,
    val isVarargs: Boolean = false,
) {

    val size
        get() = this.parameters.size

    fun match(parameters: Array<PsiParameter>, currentPosition: Int, allowCoerce: Boolean): Boolean {
        // Check if remaining parameter count is enough
        if (!isVarargs && currentPosition + size > parameters.size) {
            return false
        }

        var pos = currentPosition

        // Check parameter types
        for ((_, expectedType) in this.parameters) {
            if (isVarargs && pos == parameters.size) {
                break
            }
            val parameter = parameters[pos++]
            if (!matchParameter(expectedType, parameter, allowCoerce)) {
                return false
            }
        }

        return !isVarargs || pos == parameters.size
    }

    enum class RequiredLevel {
        OPTIONAL, WARN_IF_ABSENT, ERROR_IF_ABSENT
    }

    companion object {
        private val INT_TYPES = setOf(PsiType.INT, PsiType.SHORT, PsiType.CHAR, PsiType.BYTE, PsiType.BOOLEAN)

        private fun matchParameter(expectedType: PsiType, parameter: PsiParameter, allowCoerce: Boolean): Boolean {
            val normalizedExpected = expectedType.normalize()
            val normalizedParameter = parameter.type.normalize()
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
    }
}
