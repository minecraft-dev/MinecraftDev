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

import com.demonwav.mcdev.util.createResolveResults
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

private fun createLookup(methods: Stream<PsiMethod>, uniqueMethods: Set<String>): Array<Any> {
    return methods
            .map { m ->
                val name = if (uniqueMethods.contains(m.internalName)) {
                    m.internalName
                } else {
                    // We need to qualify the name with the descriptor
                    m.internalNameAndDescriptor
                }

                JavaLookupElementBuilder.forMethod(m, name, PsiSubstitutor.EMPTY, null)
                        .withPresentableText(m.internalName)
            }.toArray()
}

internal class MethodReferenceSingleTarget(element: PsiLiteral, val target: PsiClass) : PsiReferenceBase.Poly<PsiLiteral>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return createResolveResults(target.findMethodsByInternalNameAndDescriptor(value))
    }

    override fun getVariants(): Array<out Any> {
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

internal class MethodReferenceMultipleTargets(element: PsiLiteral, val targets: Collection<PsiClass>) :
        PsiReferenceBase.Poly<PsiLiteral>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return createResolveResults(targets.stream()
                .flatMap { it.findMethodsByInternalNameAndDescriptor(value) })
    }

    override fun getVariants(): Array<Any> {
        val methodMap = targets.stream()
                .flatMap { target -> target.methods.stream() }
                .collect(Collectors.groupingBy(PsiMethod::internalNameAndDescriptor))

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = hashSetOf<String>()
        val uniqueMethods = hashSetOf<String>()

        val allMethods = mutableListOf<PsiMethod>()

        val itr = methodMap.iterator()
        while (itr.hasNext()) {
            val (_, methods) = itr.next()

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
            if (methods.size < targets.size) {
                itr.remove()
            } else {
                allMethods.add(firstMethod)
            }
        }

        return createLookup(allMethods.stream(), uniqueMethods)
    }

}
