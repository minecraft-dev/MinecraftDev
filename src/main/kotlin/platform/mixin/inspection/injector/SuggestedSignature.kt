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

import com.demonwav.mcdev.platform.mixin.util.IntLikeAnchorSolver
import com.demonwav.mcdev.platform.mixin.util.ReturnTypeSolver
import com.demonwav.mcdev.platform.mixin.util.TypeKind
import com.demonwav.mcdev.util.SequencedSet
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.emptySequencedSet
import com.demonwav.mcdev.util.normalize
import com.demonwav.mcdev.util.reduceFallible
import com.demonwav.mcdev.util.sequencedSetOf
import com.demonwav.mcdev.util.singleDistinct
import com.demonwav.mcdev.util.singleDistinctOrNull
import com.intellij.psi.GenericsUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Type

sealed interface SignatureSuggestion {
    val params: List<Param>?
    val returnType: PsiType
    val intLikeTypes: SequencedSet<MethodSignature.TypePosition>
    val coerceReturnType: Boolean

    data class Param(val name: String?, val type: PsiType, val coerce: Boolean = false)
}

data class SuggestedReturnType(
    override val returnType: PsiType,
    val returnTypeIsIntLike: Boolean = false,
    override val coerceReturnType: Boolean = false,
) : SignatureSuggestion {
    override val intLikeTypes =
        if (returnTypeIsIntLike) sequencedSetOf(MethodSignature.TypePosition.Return) else emptySequencedSet()
    override val params get() = null

    companion object {
        /**
         * Returns a valid suggested return type iff one is possible while making no changes to the existing parameters.
         */
        fun forParams(params: PsiParameterList, signatures: List<MethodSignatures>): SuggestedReturnType? {
            val validSignatures = signatures.map { expected ->
                expected.options.filter { it.matchesParams(params) }.ifEmpty { return null }
            }

            val solver = ReturnTypeSolver(params)

            for (expected in validSignatures) {
                solver.addExpected(expected)
            }

            return solver.solve()
        }
    }
}

data class SuggestedSignature(
    override val params: List<SignatureSuggestion.Param>,
    override val returnType: PsiType,
    override val intLikeTypes: SequencedSet<MethodSignature.TypePosition> = emptySequencedSet(),
    override val coerceReturnType: Boolean = false,
) : SignatureSuggestion {
    private fun intersectCoerce(other: SuggestedSignature, manager: PsiManager): SuggestedSignature? {
        val intLikeTypes = linkedSetOf<MethodSignature.TypePosition>()

        val (returnType, returnTypeIsIntLike, coerceReturnType) = mergeTypes(
            manager,
            this.returnType,
            other.returnType,
            MethodSignature.TypePosition.Return in this.intLikeTypes,
            MethodSignature.TypePosition.Return in other.intLikeTypes,
        ) ?: return null

        if (returnTypeIsIntLike) {
            intLikeTypes.add(MethodSignature.TypePosition.Return)
        }

        return SuggestedSignature(
            params.withIndex().zip(other.params) { (index, a), b ->
                val pos = MethodSignature.TypePosition.Param(index)
                val (type, isIntLike, coerce) = mergeTypes(
                    manager,
                    a.type,
                    b.type,
                    pos in this.intLikeTypes,
                    pos in other.intLikeTypes,
                ) ?: return null
                if (isIntLike) {
                    intLikeTypes.add(pos)
                }
                val name = if (a.name == b.name) a.name else null
                SignatureSuggestion.Param(name, type, a.coerce || b.coerce || coerce)
            },
            returnType,
            SequencedSet(intLikeTypes),
            this.coerceReturnType || other.coerceReturnType || coerceReturnType,
        )
    }

    companion object {
        private fun exact(
            signature: MethodSignature,
            takeTrailing: Int = 0,
            intLikeAssignment: PsiType? = null,
        ): SuggestedSignature {
            var returnType = signature.returnType
            val params =
                (signature.requiredParams.asSequence() + signature.trailingParams.asSequence().take(takeTrailing)).map {
                    SignatureSuggestion.Param(
                        it.name,
                        it.type,
                    )
                }.toMutableList()
            if (intLikeAssignment != null) {
                for (pos in signature.intLikePositions) {
                    when (pos) {
                        MethodSignature.TypePosition.Return -> returnType = intLikeAssignment
                        is MethodSignature.TypePosition.Param -> {
                            params[pos.index] = params[pos.index].copy(type = intLikeAssignment)
                        }
                    }
                }
            }
            return SuggestedSignature(
                params,
                returnType,
                if (intLikeAssignment == null) signature.intLikePositions else emptySequencedSet(),
            )
        }

        /**
         * Returns a suggested signature for the given `@Modify`-style signatures. The resulting signature, if any, will
         * always have 1 parameter. Preference is given to the existing return type, if any, since this is likely to be
         * typed before the parameters.
         */
        fun modifier(annotation: PsiAnnotation, signatures: List<ModifierSignatures>): SuggestedSignature? {
            val parameterOptions = signatures.map { it.paramOptions }
            val psiManager = PsiManager.getInstance(annotation.project)

            val optionsByType = parameterOptions.asSequence()
                .flatMap { it.entries }
                .groupBy({ it.key }, { it.value })

            val existingReturnType = annotation.parentOfType<PsiMethod>()?.returnType
            val forExistingReturnType = existingReturnType?.let { optionsByType[Type.getType(it.descriptor)] }

            val chosenParams = forExistingReturnType?.takeIf { it.size == parameterOptions.size }
                ?: optionsByType.values.firstOrNull { it.size == parameterOptions.size } ?: return null

            val name = chosenParams.asSequence().map { it.name }.singleDistinctOrNull() ?: "original"
            val type = chosenParams.asSequence().map { it.type }
                .reduce { a, b -> GenericsUtil.getLeastUpperBound(a, b, psiManager) ?: a }

            return SuggestedSignature(listOf(SignatureSuggestion.Param(name, type)), type)
        }

        /**
         * Returns a suggested signature for the given `Operation`-wrapper signatures. Trailing parameters are never
         * used, and the Operation type is never `@Coerce`d. Doing so would make it possible to reconcile differing
         * parameter counts in some cases, but would be confusing and impractical to use.
         */
        fun operationWrapper(
            annotation: PsiAnnotation,
            signatures: List<OperationWrapperSignatures>,
        ): SuggestedSignature? {
            return intersect(
                annotation,
                signatures.map { it.signature },
                numParams = { it.requiredParams.size },
            )
        }

        /**
         * Returns a suggested signature for the given `@Inject` signatures. The target method's parameters are captured
         * if possible, and we take the longest possible prefix of the available locals from each signature. `@Coerce`
         * is used to rectify any differences in these cases, where possible. The `CallbackInfo(Returnable)` parameter
         * may be `@Coerce`d to `CallbackInfo` itself, but never to any other type. Doing so would make it possible to
         * reconcile differing parameter counts in some cases, but would be confusing and impractical to use.
         */
        fun inject(annotation: PsiAnnotation, signatures: List<InjectSignatures>): SuggestedSignature? {
            val localsToUse =
                coerciblePrefixLength(signatures.asSequence().map { sig -> sig.locals.asSequence().map { it.type } })

            val longForm = intersect(
                annotation,
                signatures.map { it.longSignature },
                numParams = { it.requiredParams.size + localsToUse },
            )

            return longForm ?: intersect(
                annotation,
                signatures.map { it.shortSignature ?: it.longSignature },
                numParams = { it.requiredParams.size },
            )
        }

        /**
         * Returns a suggested signature for the given signatures. The signature shapes are relatively flexible with
         * constraints as per the structure of [GeneralSignatures]. The resulting signature, if any, will have as many
         * parameters as the longest input signature mandates, with trailing parameters being taken from any shorter
         * signatures to fill the gaps. `@Coerce` is used to reconcile differences in parameter and return types, where
         * the input signatures allow it.
         */
        fun general(annotation: PsiAnnotation, signatures: List<GeneralSignatures>): SuggestedSignature? {
            val numParams = signatures.maxOf { it.params.size }

            val candidatesByReturnKind = signatures.asSequence()
                .flatMap { sig ->
                    sig.returnTypeOptions.keys.asSequence().map { it to sig.specificSignature(it) }
                }
                .groupBy({ it.first }, { it.second })
                .values

            for (candidates in candidatesByReturnKind) {
                if (candidates.size < signatures.size) {
                    // Not viable
                    continue
                }

                val intersected = intersect(
                    annotation,
                    candidates,
                    numParams = { numParams },
                ) ?: continue

                return intersected
            }

            return null
        }

        /**
         * Returns the most specific possible intersection of the given signatures, iff any exists. The returned
         * signature, if any, is guaranteed to satisfy all the given signatures, including when some parts do not permit
         * coercion. Parameters are taken from each signature as per [numParams].
         */
        private fun intersect(
            context: PsiElement,
            signatures: List<MethodSignature>,
            numParams: (MethodSignature) -> Int,
        ): SuggestedSignature? {
            val paramsToUse = signatures
                .map { sig ->
                    numParams(sig).also {
                        if (it !in sig.requiredParams.size..sig.requiredParams.size + sig.trailingParams.size) {
                            return null
                        }
                    }
                }
                .asSequence()
                .singleDistinctOrNull()
                ?: return null

            val intLikeAnchor = run {
                signatures.mapNotNull { it.intLikePositions.firstOrNull() }.toList()
                    .ifEmpty { return@run null }
                    .asSequence()
                    // With the currently available signature shapes, there can only ever be 1 anchor.
                    // This logic will need revisiting if that changes.
                    .singleDistinct()
            }
            val intLikeAssignment = intLikeAnchor?.let {
                val solver = IntLikeAnchorSolver()
                for (signature in signatures) {
                    for (pos in signature.allPositions(paramsToUse)) {
                        if (pos in signature.intLikePositions) {
                            continue
                        }
                        val isTrailingParam = pos is MethodSignature.TypePosition.Param &&
                            pos.index > signature.requiredParams.lastIndex
                        val allowCoerce = isTrailingParam || signature.allowCoerceRequired
                        val isValid = solver.constrain(
                            pos.getType(signature),
                            isAnchor = pos == intLikeAnchor,
                            isHard = !allowCoerce,
                        )
                        if (!isValid) {
                            return null
                        }
                    }
                }
                solver.solve()
            }

            val suggestedSignatures = signatures.map {
                exact(it, takeTrailing = paramsToUse - it.requiredParams.size, intLikeAssignment = intLikeAssignment)
            }

            if (suggestedSignatures.asSequence().zipWithNext { a, b -> !kindsMatch(a, b) }.any { it }) {
                return null
            }

            val manager = PsiManager.getInstance(context.project)
            val intersected = suggestedSignatures.asSequence()
                .reduceFallible { acc, it -> acc.intersectCoerce(it, manager) } ?: return null

            // It is fine simply to intersect and then check hard constraints, because if an input signature contains a
            // subtype Y of X at a position where X is a hard constraint, then the intersection will coerce Y upwards
            // to X. If it contains instead a *supertype* Z of X, then reconciliation is not possible and the following
            // check will fail:
            return intersected.takeIf {
                signatures.all {
                    it.allowCoerceRequired || it.matches(intersected)
                }
            }
        }
    }
}

private fun kindsMatch(a: SuggestedSignature, b: SuggestedSignature) =
    TypeKind.of(a.returnType) == TypeKind.of(b.returnType)
        && a.params.size == b.params.size
        && a.params.indices.all { TypeKind.of(a.params[it].type) == TypeKind.of(b.params[it].type) }

/**
 * Returns the largest N such that the sequences' first N types can be merged element-wise.
 */
private fun coerciblePrefixLength(types: Sequence<Sequence<PsiType>>): Int {
    val iterators = types.map { it.iterator() }.toList()
    var i = 0

    while (true) {
        var kind: TypeKind? = null
        var forcedIntType: PsiType? = null

        for (iterator in iterators) {
            val candidate = if (iterator.hasNext()) iterator.next() else return i
            val candidateKind = TypeKind.of(candidate)

            when (kind) {
                null -> kind = candidateKind
                candidateKind -> {}
                else -> return i
            }

            if (candidateKind == TypeKind.INT_LIKE && candidate != PsiTypes.intType()) {
                when (forcedIntType) {
                    null -> forcedIntType = candidate
                    candidate -> {}
                    else -> return i
                }
            }
        }

        i++
    }
}

/**
 * Merges the two types with regards to `@Coerce` behaviour. Returns the merged type, whether the merged type is a
 * free int-like type, and whether the merge **newly** requires `@Coerce`.
 *
 * **Preconditions:** The types must be of the same kind, and if either type is a free int-like type, the other must be
 * too. (Fully solving int-like types is left to the caller.)
 */
private fun mergeTypes(
    manager: PsiManager,
    a: PsiType,
    b: PsiType,
    aIsIntLike: Boolean,
    bIsIntLike: Boolean,
): TypeMergeResult? = when (TypeKind.of(a)) {
    TypeKind.OBJECT -> {
        TypeMergeResult(
            GenericsUtil.getLeastUpperBound(a, b, manager)!!,
            isIntLike = false,
            coerce = a.normalize() != b.normalize(),
        )
    }

    TypeKind.INT_LIKE -> {
        when {
            aIsIntLike && bIsIntLike -> TypeMergeResult(a, isIntLike = true, coerce = false)
            aIsIntLike || bIsIntLike -> error("Int-like type should have been forced")
            a == b -> TypeMergeResult(a, isIntLike = false, coerce = false)
            a == PsiTypes.intType() -> TypeMergeResult(b, isIntLike = false, coerce = true)
            b == PsiTypes.intType() -> TypeMergeResult(a, isIntLike = false, coerce = true)
            else -> null
        }
    }

    else -> {
        require(a == b)
        TypeMergeResult(a, isIntLike = false, coerce = false)
    }
}

private data class TypeMergeResult(val type: PsiType, val isIntLike: Boolean, val coerce: Boolean)
