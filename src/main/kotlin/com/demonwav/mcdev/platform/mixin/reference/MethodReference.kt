/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.platform.mixin.util.findUpstreamMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.memberReference
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.toResolveResults
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

object MethodReference : PolyReferenceResolver(), MixinReference {

    override val description: String
        get() = "method '%s' in target class"

    override fun isValidAnnotation(name: String) = name in METHOD_INJECTORS

    private fun getTargets(context: PsiElement): Collection<PsiClass>? {
        val psiClass = context.findContainingClass() ?: return null
        val targets = psiClass.mixinTargets
        val upstreamMixin = context.findContainingMethod()?.findUpstreamMixin()
        return when {
            targets.isEmpty() -> listOfNotNull(upstreamMixin)
            upstreamMixin != null -> targets + upstreamMixin
            else -> targets
        }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val targetMethodInfo = MixinMemberReference.parse(context.constantStringValue) ?: return false
        val targets = getTargets(context) ?: return false
        return !targets.stream().flatMap { it.findMethods(targetMethodInfo) }.findAny().isPresent
    }

    fun getReferenceIfAmbiguous(context: PsiElement): MemberReference? {
        val targetReference = MixinMemberReference.parse(context.constantStringValue) ?: return null
        if (targetReference.descriptor != null) {
            return null
        }

        val targets = getTargets(context) ?: return null
        return if (isAmbiguous(targets, targetReference)) targetReference else null
    }

    private fun isAmbiguous(targets: Collection<PsiClass>, targetReference: MemberReference): Boolean {
        return targets.any { it.findMethodsByName(targetReference.name, false).size > 1 }
    }

    private fun resolve(context: PsiElement): Stream<PsiMethod>? {
        val targetReference = MixinMemberReference.parse(context.constantStringValue) ?: return null
        val targets = getTargets(context) ?: return null
        return resolve(targets, targetReference)
    }

    private fun resolve(targets: Collection<PsiClass>, targetReference: MemberReference): Stream<PsiMethod> {
        return targets.stream()
            .flatMap { it.findMethods(targetReference) }
    }

    fun resolveIfUnique(context: PsiElement): PsiMethod? {
        return resolve(context)?.collect(Collectors.reducing<PsiMethod> { _, _ -> null })?.orElse(null)
    }

    fun resolveAllIfNotAmbiguous(context: PsiElement): List<PsiMethod>? {
        val targetReference = MixinMemberReference.parse(context.constantStringValue) ?: return null
        val targets = getTargets(context) ?: return null

        if (targetReference.descriptor == null && isAmbiguous(targets, targetReference)) {
            return null
        }

        return resolve(targets, targetReference).toList()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        return resolve(context)?.toResolveResults() ?: ResolveResult.EMPTY_ARRAY
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val targets = getTargets(context) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return targets.singleOrNull()?.let { collectVariants(context, it) } ?: collectVariants(context, targets)
    }

    private fun collectVariants(context: PsiElement, target: PsiClass): Array<Any> {
        val methods = target.methods

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = HashSet<String>()
        val uniqueMethods = HashSet<String>()

        for (method in methods) {
            val name = method.internalName
            if (visitedMethods.add(name)) {
                uniqueMethods.add(name)
            } else {
                uniqueMethods.remove(name)
            }
        }

        return createLookup(context, methods.stream(), uniqueMethods)
    }

    private fun collectVariants(context: PsiElement, targets: Collection<PsiClass>): Array<Any> {
        val groupedMethods = targets.stream()
            .flatMap { target -> target.methods.stream() }
            .collect(Collectors.groupingBy(PsiMethod::memberReference))
            .values

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = HashSet<String>()
        val uniqueMethods = HashSet<String>()

        val allMethods = ArrayList<PsiMethod>(groupedMethods.size)

        for (methods in groupedMethods) {
            val firstMethod = methods.first()
            val name = firstMethod.internalName
            if (visitedMethods.add(name)) {
                uniqueMethods.add(name)
            } else {
                uniqueMethods.remove(name)
            }

            // If we have a method with the same name and descriptor in at least
            // as many classes as targets it should be present in all of them.
            // Not sure how you would have more methods than targets but who cares.
            if (methods.size >= targets.size) {
                allMethods.add(firstMethod)
            }
        }

        return createLookup(context, allMethods.stream(), uniqueMethods)
    }

    private fun createLookup(context: PsiElement, methods: Stream<PsiMethod>, uniqueMethods: Set<String>): Array<Any> {
        return methods
            .map { m ->
                val targetMethodInfo = if (m.internalName in uniqueMethods) {
                    MemberReference(m.internalName)
                } else {
                    m.memberReference
                }

                JavaLookupElementBuilder.forMethod(
                    m,
                    MixinMemberReference.toString(targetMethodInfo),
                    PsiSubstitutor.EMPTY,
                    null
                )
                    .withPresentableText(m.internalName)
                    .completeToLiteral(context)
            }.toArray()
    }
}
