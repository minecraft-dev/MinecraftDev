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

import com.demonwav.mcdev.platform.mixin.util.TypeKind
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.platform.mixin.util.mixinExtrasOperationType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.SequencedMap
import com.demonwav.mcdev.util.SequencedSet
import com.demonwav.mcdev.util.emptySequencedSet
import com.demonwav.mcdev.util.minus
import com.demonwav.mcdev.util.sequencedMapOf
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import org.objectweb.asm.Type

sealed interface ExpectedSignatures<out T : MethodSignatures> {
    fun matches(method: PsiMethod): Boolean

    data object Unknown : ExpectedSignatures<Nothing> {
        override fun matches(method: PsiMethod) = true
    }

    data object Invalid : ExpectedSignatures<Nothing> {
        override fun matches(method: PsiMethod) = false
    }

    data class Valid<out T : MethodSignatures>(val expected: T) : ExpectedSignatures<T> {
        override fun matches(method: PsiMethod) = expected.options.any { it.matches(method) }
    }
}

inline fun <reified R : MethodSignatures> List<ExpectedSignatures<*>>.collectSignatures(): List<R>? =
    mapNotNull {
        when (it) {
            ExpectedSignatures.Unknown -> null
            ExpectedSignatures.Invalid -> return null
            is ExpectedSignatures.Valid -> it.expected as R
        }
    }

interface MethodSignatures {
    val options: List<MethodSignature>
}

class ModifierSignatures(
    val paramOptions: SequencedMap<Type, Parameter>,
    val fullParams: List<Parameter>? = null,
    val trailingParams: List<Parameter> = emptyList(),
) : MethodSignatures {
    override val options = paramOptions.values.flatMap { param ->
        listOfNotNull(
            MethodSignature(
                listOf(param),
                param.type,
                allowCoerceRequired = false,
                trailingParams = trailingParams,
            ),
            fullParams?.let {
                MethodSignature(
                    it,
                    param.type,
                    allowCoerceRequired = false,
                    trailingParams = trailingParams,
                )
            },
        )
    }
}

class BasicSignatures(override val options: List<MethodSignature>) : MethodSignatures {
    constructor(vararg options: MethodSignature) : this(options.asList())
}

class OperationWrapperSignatures private constructor(
    val params: List<Parameter>,
    val returnType: PsiType,
    val intLikePositions: SequencedSet<MethodSignature.TypePosition>,
    val trailingParams: List<Parameter>,
    operationType: PsiType,
) : MethodSignatures {
    val signature = MethodSignature(
        params + Parameter("original", operationType),
        returnType,
        allowCoerceRequired = true,
        trailingParams = trailingParams,
        intLikePositions = intLikePositions,
    )

    override val options = listOf(signature)

    companion object {
        operator fun invoke(
            context: PsiElement,
            params: List<Parameter>,
            returnType: PsiType,
            intLikePositions: SequencedSet<MethodSignature.TypePosition> = emptySequencedSet(),
            trailingParams: List<Parameter> = emptyList(),
        ) = mixinExtrasOperationType(context, returnType)?.let {
            OperationWrapperSignatures(params, returnType, intLikePositions, trailingParams, it)
        }
    }
}

data class GeneralSignatures(
    val params: List<Parameter>,
    val returnTypeOptions: SequencedMap<TypeKind, PsiType>,
    val allowCoerce: Boolean,
    val trailingParams: List<Parameter>,
    val intLikePositions: SequencedSet<MethodSignature.TypePosition> = emptySequencedSet(),
) : MethodSignatures {
    constructor(
        params: List<Parameter>,
        returnType: PsiType,
        trailingParams: List<Parameter>,
        allowCoerce: Boolean = true,
        intLikePositions: SequencedSet<MethodSignature.TypePosition> = emptySequencedSet(),
    ) : this(
        params,
        sequencedMapOf(TypeKind.of(returnType) to returnType),
        allowCoerce,
        trailingParams,
        intLikePositions,
    )

    override val options = returnTypeOptions.keys.map(::specificSignature)

    fun specificSignature(returnKind: TypeKind) = MethodSignature(
        params,
        returnTypeOptions.getValue(returnKind),
        allowCoerceRequired = allowCoerce,
        trailingParams = trailingParams,
        intLikePositions = if (returnKind == TypeKind.INT_LIKE) {
            intLikePositions
        } else {
            intLikePositions - MethodSignature.TypePosition.Return
        },
    )
}

class InjectSignatures(
    val params: List<Parameter>,
    val locals: List<Parameter>,
    ciParam: Parameter,
) : MethodSignatures {
    val shortSignature = if (params.isNotEmpty()) {
        MethodSignature(
            listOf(ciParam),
            PsiTypes.voidType(),
            allowCoerceRequired = true,
        )
    } else {
        null
    }

    val longSignature = MethodSignature(
        params + ciParam,
        PsiTypes.voidType(),
        allowCoerceRequired = true,
        trailingParams = locals,
    )

    override val options = listOfNotNull(shortSignature, longSignature)

    companion object {
        operator fun invoke(
            context: PsiElement,
            params: List<Parameter>,
            returnType: PsiType,
            locals: List<Parameter>
        ): InjectSignatures? {
            val ciParam = if (returnType == PsiTypes.voidType()) {
                Parameter("ci", callbackInfoType(context.project))
            } else {
                Parameter(
                    "cir",
                    callbackInfoReturnableType(context.project, context, returnType) ?: return null,
                )
            }
            return InjectSignatures(params, locals, ciParam)
        }
    }
}
