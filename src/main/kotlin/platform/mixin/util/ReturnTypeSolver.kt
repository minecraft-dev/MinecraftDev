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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedReturnType
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.COERCE
import com.demonwav.mcdev.util.MutableSequencedMap
import com.demonwav.mcdev.util.PrioritySet
import com.demonwav.mcdev.util.normalize
import com.demonwav.mcdev.util.singleDistinct
import com.intellij.psi.CommonClassNames
import com.intellij.psi.GenericsUtil
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes

/**
 * Represents a constraint solver for the return type of a signature given a fixed list of parameters.
 */
class ReturnTypeSolver(private val parameterList: PsiParameterList) {
    private val manager = PsiManager.getInstance(parameterList.project)
    private val objectReturnOptionsCache = hashMapOf<PsiType, List<Prioritized<SuggestedReturnType>>>()
    private val suggestionsByType = hashMapOf<TypeKey, MutableSequencedMap<Int, SuggestedReturnType>>()
    private val typeOptions = PrioritySet<TypeKey>()
    private var numExpected = 0

    /**
     * Adds a new constraint that the chosen return type must match at least one of the given signatures.
     */
    fun addExpected(signatures: List<MethodSignature>) {
        val expectedIndex = numExpected++
        for (signature in signatures) {
            for ((option, priority) in signature.returnTypeOptions()) {
                val key = TypeKey.of(option)
                typeOptions.add(key, priority)
                suggestionsByType.getOrPut(key, ::linkedMapOf).putIfAbsent(expectedIndex, option)
            }
        }
    }

    /**
     * Returns a suggested return type, or null if it is not possible to satisfy all the constraints.
     */
    fun solve(): SuggestedReturnType? {
        val suggestions = typeOptions.asSequence()
            .map { suggestionsByType.getValue(it) }.firstOrNull { it.size == numExpected }?.values
        return suggestions?.let { intersect(it) }
    }

    /**
     * Returns the possible return types that this signature can support, along with their priority.
     *
     * **Precondition:** The parameter list is valid according to the signature.
     */
    private fun MethodSignature.returnTypeOptions(): List<Prioritized<SuggestedReturnType>> {
        if (MethodSignature.TypePosition.Return in intLikePositions) {
            val params = parameterList.parameters
            val anchor = (intLikePositions.first() as? MethodSignature.TypePosition.Param)
                ?.let { params[it.index].type }
            return when (anchor) {
                PsiTypes.intType() -> {
                    intReturnOptions(allowCoerceRequired)
                }
                null -> {
                    // The return type itself is the anchor
                    val intLikeParams = intLikePositions.asSequence()
                        .mapNotNull { it.getParam(params) }
                        .groupBy { it.type }
                    when {
                        intLikeParams.isEmpty() -> {
                            // Only the return type is int-like, free choice
                            val results = mutableListOf(
                                SuggestedReturnType(PsiTypes.intType(), returnTypeIsIntLike = true) withPriority 0
                            )
                            for (specific in intLikeTypes) {
                                // We don't know what the return type should be, so we avoid influencing the preference
                                // order.
                                results.add(SuggestedReturnType(specific) withPriority Int.MAX_VALUE)
                            }
                            results
                        }
                        PsiTypes.intType() in intLikeParams -> {
                            // Only int can be coerced to int (can also be coerced to anything else we found)
                            listOf(SuggestedReturnType(PsiTypes.intType()) withPriority 0)
                        }
                        intLikeParams.size == 1 -> {
                            // Take the specific leaf type we found, plus int iff all the params have @Coerce.
                            // int is less preferable since it doesn't match exactly.
                            val leaf = intLikeParams.keys.single()
                            val results = mutableListOf(
                                SuggestedReturnType(leaf) withPriority 0
                            )
                            if (allowCoerceRequired && intLikeParams.values.single().all { it.hasAnnotation(COERCE) }) {
                                results.add(
                                    SuggestedReturnType(PsiTypes.intType()) withPriority 1
                                )
                            }
                            results
                        }
                        else -> {
                            // Only int can be coerced to multiple types
                            check(allowCoerceRequired)
                            for (params in intLikeParams.values) {
                                // We double-check that the parameters are valid as the caller promised.
                                // NB We know there are no int params, so all must be coerced.
                                check(params.all { it.hasAnnotation(COERCE) })
                            }
                            listOf(SuggestedReturnType(PsiTypes.intType()) withPriority 0)
                        }
                    }
                }
                else -> {
                    // Anchor is a leaf type which can only be coerced to itself
                    listOf(SuggestedReturnType(anchor) withPriority 0)
                }
            }
        }
        return when {
            returnType == PsiTypes.intType() -> intReturnOptions(allowCoerceRequired)
            TypeKind.of(returnType) == TypeKind.OBJECT -> objectReturnOptions(returnType, allowCoerceRequired)
            else -> listOf(SuggestedReturnType(returnType) withPriority 0)
        }
    }

    /**
     * Returns a suggested return type which is the LUB of all the given types.
     *
     * **Precondition:** [suggestions] must be non-empty and every suggestion in [suggestions] must have the same type
     * as considered by [TypeKey].
     */
    private fun intersect(suggestions: Iterable<SuggestedReturnType>): SuggestedReturnType {
        val types = suggestions.asSequence().map { it.returnType }
        val kind = types.map { TypeKind.of(it) }.singleDistinct()
        val mergedType = when (kind) {
            TypeKind.OBJECT -> types.reduce { acc, it -> GenericsUtil.getLeastUpperBound(acc, it, manager)!! }
            else -> types.singleDistinct()
        }
        return SuggestedReturnType(
            mergedType,
            returnTypeIsIntLike = suggestions.asSequence().map { it.returnTypeIsIntLike }.singleDistinct(),
            coerceReturnType = suggestions.any { it.coerceReturnType },
        )
    }

    /**
     * Yields all valid supertypes (inclusive) of the given type, at most once per raw type, with priority equal to the
     * number of traversal steps required to reach the supertype.
     */
    private fun objectReturnOptions(type: PsiType, allowCoerce: Boolean): List<Prioritized<SuggestedReturnType>> =
        if (!allowCoerce) {
            listOf(SuggestedReturnType(type) withPriority 0)
        } else {
            objectReturnOptionsCache.getOrPut(type) {
                val result = mutableListOf<Prioritized<SuggestedReturnType>>()

                val queue = ArrayDeque(listOf(SuggestedReturnType(type) withPriority 0))
                val visited = hashSetOf(type.normalize())

                while (queue.isNotEmpty()) {
                    val (next, priority) = queue.removeFirst().also { result.add(it) }
                    for (directSuper in next.returnType.directSupertypes()) {
                        if (visited.add(directSuper.normalize())) {
                            queue.addLast(
                                SuggestedReturnType(
                                    directSuper,
                                    coerceReturnType = true,
                                ) withPriority priority + 1
                            )
                        }
                    }
                }

                result
            }
        }

    private fun PsiType.directSupertypes(): List<PsiType> = when (this) {
        is PsiArrayType -> {
            if (componentType is PsiPrimitiveType || componentType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)) {
                arraySuperTypes.map { PsiType.getTypeByName(it, manager.project, parameterList.resolveScope) }
            } else {
                componentType.directSupertypes().map { it.createArrayType() }
            }
        }
        else -> {
            val superTypeArray = superTypes
            if (superTypeArray.size <= 1) {
                superTypeArray.toList()
            } else {
                // Delay java.lang.Object since it is necessarily less specific than any other type
                superTypeArray.filterNot { it.equalsToText(CommonClassNames.JAVA_LANG_OBJECT) }
            }
        }
    }

    private companion object {
        private val intLikeTypes = listOf(
            PsiTypes.intType(),
            PsiTypes.booleanType(),
            PsiTypes.charType(),
            PsiTypes.byteType(),
            PsiTypes.shortType(),
        )

        private val intReturnOptions = intLikeTypes.map {
            val needsCoerce = it != PsiTypes.intType()
            SuggestedReturnType(
                it,
                coerceReturnType = needsCoerce,
            ) withPriority (if (needsCoerce) 1 else 0)
        }

        private val arraySuperTypes = listOf(
            CommonClassNames.JAVA_IO_SERIALIZABLE,
            CommonClassNames.JAVA_LANG_CLONEABLE,
        )

        private fun intReturnOptions(allowCoerce: Boolean): List<Prioritized<SuggestedReturnType>> =
            if (allowCoerce) intReturnOptions else listOf(SuggestedReturnType(PsiTypes.intType()) withPriority 0)
    }
}

private data class Prioritized<out T>(val element: T, val priority: Int)

private infix fun <T> T.withPriority(priority: Int) = Prioritized(this, priority)

private sealed interface TypeKey {
    data class Type(val type: PsiType) : TypeKey

    data object IntLike : TypeKey

    companion object {
        fun of(suggestion: SuggestedReturnType) = if (suggestion.returnTypeIsIntLike) {
            IntLike
        } else {
            Type(suggestion.returnType.normalize())
        }
    }
}
