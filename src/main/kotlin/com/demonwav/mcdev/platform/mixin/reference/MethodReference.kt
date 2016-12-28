/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.createResolveResults
import com.demonwav.mcdev.util.findMethodsByInternalName
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

internal class MixinMethodReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val reference = createMethodReference(element) ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(reference)
    }

}

fun createMethodReference(element: PsiElement): MixinReference? {
    val mixinClass = getClassOfElement(element) ?: return null
    val targets = MixinUtils.getAllMixedClasses(mixinClass).values

    return when (targets.size) {
        0 -> null
        1 -> MethodReferenceSingleTarget(element, targets.single())
        else -> MethodReferenceMultipleTargets(element, targets)
    }
}
private fun PsiClass.findMethodsForValue(value: String): Stream<PsiMethod> {
    if (value.endsWith('*')) {
        return findMethodsByInternalName(value.substring(0, value.length - 1))
    } else {
        return findMethodsByInternalNameAndDescriptor(value)
    }
}

private abstract class MethodReference(element: PsiElement) : ConstantLiteralReference.Poly(element) {

    protected fun createLookup(methods: Stream<PsiMethod>, uniqueMethods: Set<String>): Array<Any> {
        return methods
                .map { m ->
                    val name = if (m.internalName in uniqueMethods) {
                        m.internalName
                    } else {
                        // We need to qualify the name with the descriptor
                        m.internalNameAndDescriptor
                    }

                    patchLookup(JavaLookupElementBuilder.forMethod(m, name, PsiSubstitutor.EMPTY, null)
                            .withPresentableText(m.internalName))
                }.toArray()
    }

}

private class MethodReferenceSingleTarget(element: PsiElement, val target: PsiClass) : MethodReference(element) {

    override val description: String
        get() = "method '$value' in target class"

    override fun validate(results: Array<ResolveResult>): MixinReference.State {
        val result = super.validate(results)
        return if (result == MixinReference.State.AMBIGUOUS && value.endsWith('*')) {
            MixinReference.State.VALID
        } else {
            result
        }
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return createResolveResults(target.findMethodsForValue(value))
    }

    override fun getVariants(): Array<Any> {
        val methods = target.methods

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = hashSetOf<String>()
        val uniqueMethods = hashSetOf<String>()

        for (method in methods) {
            val name = method.internalName
            if (visitedMethods.add(name)) {
                uniqueMethods.add(name)
            } else {
                uniqueMethods.remove(name)
            }
        }

        return createLookup(methods.stream(), uniqueMethods)
    }

}

private class MethodReferenceMultipleTargets(element: PsiElement, val targets: Collection<PsiClass>) : MethodReference(element) {

    override val description: String
        get() = "method '$value' in all target classes"

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return createResolveResults(targets.stream()
                .flatMap { it.findMethodsForValue(value) })
    }

    override fun validate(results: Array<ResolveResult>) = when (results.size) {
        0 -> MixinReference.State.UNRESOLVED
        targets.size -> MixinReference.State.VALID
        else -> MixinReference.State.UNRESOLVED
        // TODO: Handle ambiguous references for Mixins with multiple targets
    }

    override fun getVariants(): Array<Any> {
        val methodMap = targets.stream()
                .flatMap { target -> target.methods.stream() }
                .collect(Collectors.groupingBy(PsiMethod::internalNameAndDescriptor))

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = hashSetOf<String>()
        val uniqueMethods = hashSetOf<String>()

        val allMethods = mutableListOf<PsiMethod>()

        for ((_, methods) in methodMap) {
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

        return createLookup(allMethods.stream(), uniqueMethods)
    }

}
