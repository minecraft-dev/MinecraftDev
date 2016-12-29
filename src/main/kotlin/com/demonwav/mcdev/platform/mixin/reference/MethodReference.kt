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
import com.demonwav.mcdev.util.MemberDescriptor
import com.demonwav.mcdev.util.createResolveResults
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.memberDescriptor
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

private abstract class MethodReference(element: PsiElement) : ConstantLiteralReference.Poly(element) {

    protected fun createLookup(methods: Stream<PsiMethod>, uniqueMethods: Set<String>): Array<Any> {
        return methods
                .map { m ->
                    val descriptor = if (m.internalName in uniqueMethods) {
                        MemberDescriptor(m.internalName)
                    } else {
                        m.memberDescriptor
                    }

                    patchLookup(JavaLookupElementBuilder.forMethod(m, descriptor.toString(), PsiSubstitutor.EMPTY, null)
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
        val member = MemberDescriptor.parse(value) ?: return ResolveResult.EMPTY_ARRAY
        return createResolveResults(target.findMethods(member))
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
        val member = MemberDescriptor.parse(value) ?: return ResolveResult.EMPTY_ARRAY
        return createResolveResults(targets.stream()
                .flatMap { it.findMethods(member) })
    }

    override fun validate(results: Array<ResolveResult>): MixinReference.State {
        // TODO: Verify if target method is present it all targets?
        return if (multiResolve(false).isEmpty()) MixinReference.State.UNRESOLVED else MixinReference.State.VALID
    }

    override fun getVariants(): Array<Any> {
        val methodMap = targets.stream()
                .flatMap { target -> target.methods.stream() }
                .collect(Collectors.groupingBy(PsiMethod::memberDescriptor))

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
