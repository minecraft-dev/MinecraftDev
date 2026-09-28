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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.platform.mixin.util.checkCoerce
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.SequencedSet
import com.demonwav.mcdev.util.allEqual
import com.demonwav.mcdev.util.emptySequencedSet
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypes

data class MethodSignature(
    val requiredParams: List<Parameter>,
    val returnType: PsiType,
    val allowCoerceRequired: Boolean,
    val trailingParams: List<Parameter> = emptyList(),
    val intLikePositions: SequencedSet<TypePosition> = emptySequencedSet(),
) {
    sealed interface TypePosition {
        fun getElement(method: PsiMethod): PsiTypeElement?

        fun getType(signature: MethodSignature): PsiType

        fun getParam(params: Array<PsiParameter>): PsiParameter?

        data object Return : TypePosition {
            override fun getElement(method: PsiMethod) = method.returnTypeElement

            override fun getType(signature: MethodSignature) = signature.returnType

            override fun getParam(params: Array<PsiParameter>) = null
        }

        data class Param(val index: Int) : TypePosition {
            override fun getElement(method: PsiMethod) = method.parameterList.parameters[index].typeElement

            override fun getType(signature: MethodSignature) = signature.requiredParams[index].type

            override fun getParam(params: Array<PsiParameter>) = params[index]
        }
    }

    fun allPositions(numParams: Int): Sequence<TypePosition> {
        require(numParams >= requiredParams.size)
        return sequenceOf(TypePosition.Return) + (0..<numParams).map { TypePosition.Param(it) }
    }

    fun matches(method: PsiMethod): Boolean {
        return matches(
            method.parameterList.parameters.dropLastWhile { it.isMixinExtrasSugar },
            method.returnType ?: return false,
            method.hasAnnotation(COERCE),
            { it.type },
            { it.hasAnnotation(COERCE) },
        )
    }

    fun matches(suggested: SuggestedSignature): Boolean {
        return matches(
            suggested.params,
            suggested.returnType,
            suggested.coerceReturnType,
            { it.type },
            { it.coerce },
        )
    }

    fun matchesParams(params: PsiParameterList): Boolean {
        return matchesParams(
            params.parameters.dropLastWhile { it.isMixinExtrasSugar },
            { it.type },
            { it.hasAnnotation(COERCE) },
            knownIntLikeAssignment = null,
        )
    }

    fun matchesReturnType(returnType: PsiType, hasCoerce: Boolean): Boolean =
        matchType(
            this.returnType,
            returnType,
            coerce = allowCoerceRequired && hasCoerce,
            isIntLike = TypePosition.Return in intLikePositions,
        )

    private fun <ParamT : Any> matches(
        params: List<ParamT>,
        returnType: PsiType,
        returnCoerce: Boolean,
        paramType: (ParamT) -> PsiType,
        paramCoerce: (ParamT) -> Boolean,
    ): Boolean {
        val intLikeAssignment = when (val anchor = intLikePositions.firstOrNull()) {
            null -> null
            is TypePosition.Param -> paramType(params.getOrNull(anchor.index) ?: return false)
            TypePosition.Return -> returnType
        }
        val transformedReturnType = if (TypePosition.Return in intLikePositions) intLikeAssignment!! else returnType
        return matchesReturnType(transformedReturnType, returnCoerce)
            && matchesParams(
            params,
            paramType,
            paramCoerce,
            knownIntLikeAssignment = intLikeAssignment,
        )
    }

    private fun <ParamT : Any> matchesParams(
        params: List<ParamT>,
        paramType: (ParamT) -> PsiType,
        paramCoerce: (ParamT) -> Boolean,
        knownIntLikeAssignment: PsiType?,
    ): Boolean {
        if (params.size !in requiredParams.size..requiredParams.size + trailingParams.size) {
            return false
        }

        val intLikeAssignment = knownIntLikeAssignment
            ?: (intLikePositions.firstOrNull() as? TypePosition.Param)?.let { paramType(params[it.index]) }

        if (intLikeAssignment == null && intLikePositions.isNotEmpty()) {
            // We don't know the return type, but we should make sure the combination is feasible for some return type
            val intLikeIndices = intLikePositions.mapNotNull { (it as? TypePosition.Param)?.index }
            val isFeasible = intLikeIndices.asSequence().map { paramType(params[it]) }.allEqual()
                || intLikeIndices.all { index ->
                val param = params[index]
                checkCoerce(
                    PsiTypes.intType(),
                    paramType(param),
                    coerce = paramCoerce(param),
                    expectedIntLike = false,
                )
            }
            if (!isFeasible) {
                return false
            }
        }

        fun matchParams(expected: List<Parameter>, allowCoerce: Boolean, startIndex: Int): Boolean {
            return expected.asSequence()
                .zip(params.asSequence().withIndex().drop(startIndex))
                .all { (expected, indexAndActual) ->
                    val (index, actual) = indexAndActual
                    matchType(
                        expected.type,
                        paramType(actual),
                        isIntLike = intLikeAssignment == null && TypePosition.Param(index) in intLikePositions,
                        coerce = allowCoerce && paramCoerce(actual),
                    )
                }
        }

        val transformedRequiredParams = if (intLikeAssignment == null) {
            requiredParams
        } else {
            requiredParams.mapIndexed { i, param ->
                if (TypePosition.Param(i) in intLikePositions) {
                    param.copy(type = intLikeAssignment)
                } else {
                    param
                }
            }
        }

        return matchParams(transformedRequiredParams, allowCoerceRequired, 0)
            && matchParams(trailingParams, true, requiredParams.size)
    }

    private companion object {
        private fun matchType(expected: PsiType, actual: PsiType, isIntLike: Boolean, coerce: Boolean) =
            checkCoerce(expected, actual, coerce, isIntLike)
    }
}
