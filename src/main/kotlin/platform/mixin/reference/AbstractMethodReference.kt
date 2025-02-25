/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.findUpstreamMixin
import com.demonwav.mcdev.platform.mixin.util.memberReference
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.toResolveResults
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import org.objectweb.asm.tree.ClassNode

/**
 * The reference inside e.g. @Inject.method(). Similar to [TargetReference], this reference has different ways of being
 * resolved. See the docs for that class for details.
 */
abstract class AbstractMethodReference : PolyReferenceResolver(), MixinReference {
    abstract fun parseSelector(context: PsiElement): MixinSelector?

    open fun parseSelector(stringValue: String, context: PsiElement): MixinSelector? {
        return parseSelector(context)
    }

    protected open fun getTargets(context: PsiElement): Collection<ClassNode>? {
        val psiClass = context.findContainingClass() ?: return null
        val targets = psiClass.mixinTargets
        val upstreamMixin = context.findContainingMethod()?.findUpstreamMixin()?.bytecode
        return when {
            upstreamMixin != null -> targets + upstreamMixin
            else -> targets
        }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        // check if the annotation handler is soft
        val annotation = context.parentOfType<PsiAnnotation>()
        if (annotation != null && MixinAnnotationHandler.forMixinAnnotation(annotation)?.isSoft == true) {
            return false
        }

        val stringValue = context.constantStringValue ?: return false
        val targetMethodInfo = parseSelector(stringValue, context) ?: return false
        val targets = getTargets(context) ?: return false
        return !targets.asSequence().flatMap {
            targetMethodInfo.getCustomOwner(it).findMethods(targetMethodInfo)
        }.any()
    }

    fun getReferenceIfAmbiguous(context: PsiElement): MemberReference? {
        val targetReference = parseSelector(context) as? MemberReference ?: return null
        if (targetReference.descriptor != null) {
            return null
        }

        val targets = getTargets(context) ?: return null
        return if (isAmbiguous(targets, targetReference)) targetReference else null
    }

    private fun isAmbiguous(targets: Collection<ClassNode>, targetReference: MemberReference): Boolean {
        if (targetReference.matchAllNames) {
            return targets.any {
                val methods = it.methods
                methods != null && methods.size > 1
            }
        }
        return targets.any { it.findMethods(MemberReference(targetReference.name)).count() > 1 }
    }

    private fun resolve(context: PsiElement): Sequence<ClassAndMethodNode>? {
        val targets = getTargets(context) ?: return null
        val targetedMethods = when (context) {
            is PsiArrayInitializerMemberValue -> context.initializers.mapNotNull { it.constantStringValue }
            else -> context.constantStringValue?.let { listOf(it) } ?: emptyList()
        }

        return targetedMethods.asSequence().flatMap { method ->
            val targetReference = parseSelector(method, context) ?: return@flatMap emptySequence()
            return@flatMap resolve(targets, targetReference)
        }
    }

    private fun resolve(
        targets: Collection<ClassNode>,
        selector: MixinSelector,
    ): Sequence<ClassAndMethodNode> {
        return targets.asSequence()
            .flatMap { target ->
                val actualTarget = selector.getCustomOwner(target)
                actualTarget.findMethods(selector).map { ClassAndMethodNode(actualTarget, it) }
            }
    }

    fun resolveIfUnique(context: PsiElement): ClassAndMethodNode? {
        return resolve(context)?.singleOrNull()
    }

    fun resolveAllIfNotAmbiguous(context: PsiElement): List<ClassAndMethodNode>? {
        val targets = getTargets(context) ?: return null

        val targetedMethods = when (context) {
            is PsiArrayInitializerMemberValue -> context.initializers.mapNotNull { it.constantStringValue }
            else -> context.constantStringValue?.let { listOf(it) } ?: emptyList()
        }

        return targetedMethods.asSequence().flatMap { method ->
            val targetReference = parseSelector(method, context) ?: return@flatMap emptySequence()
            if (targetReference is MemberReference && targetReference.descriptor == null && isAmbiguous(
                    targets,
                    targetReference,
                )
            ) {
                return@flatMap emptySequence()
            }
            return@flatMap resolve(targets, targetReference)
        }.toList()
    }

    fun resolveForNavigation(context: PsiElement): Array<PsiElement>? {
        return resolve(context)?.mapNotNull {
            it.method.findSourceElement(
                it.clazz,
                context.project,
                scope = context.resolveScope,
                canDecompile = true,
            )
        }?.toTypedArray()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        return resolve(context)?.mapNotNull {
            it.method.findSourceElement(
                it.clazz,
                context.project,
                scope = context.resolveScope,
                canDecompile = false,
            )
        }?.toResolveResults() ?: ResolveResult.EMPTY_ARRAY
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val targets = getTargets(context) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return targets.singleOrNull()?.let { collectVariants(context, it) } ?: collectVariants(context, targets)
    }

    private fun collectVariants(context: PsiElement, target: ClassNode): Array<Any> {
        val methods = target.methods ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = HashSet<String>()
        val uniqueMethods = HashSet<String>()

        for (method in methods) {
            val name = method.name
            if (visitedMethods.add(name)) {
                uniqueMethods.add(name)
            } else {
                uniqueMethods.remove(name)
            }
        }

        return createLookup(context, methods.asSequence().map { ClassAndMethodNode(target, it) }, uniqueMethods)
    }

    private fun collectVariants(context: PsiElement, targets: Collection<ClassNode>): Array<Any> {
        val groupedMethods = targets.asSequence()
            .flatMap { target ->
                target.methods?.asSequence()?.map { ClassAndMethodNode(target, it) } ?: emptySequence()
            }
            .groupBy { it.method.memberReference }
            .values

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = HashSet<String>()
        val uniqueMethods = HashSet<String>()

        val allMethods = ArrayList<ClassAndMethodNode>(groupedMethods.size)

        for (methods in groupedMethods) {
            val firstMethod = methods.first()
            val name = firstMethod.method.name
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

        return createLookup(context, allMethods.asSequence(), uniqueMethods)
    }

    private fun createLookup(
        context: PsiElement,
        methods: Sequence<ClassAndMethodNode>,
        uniqueMethods: Set<String>,
    ): Array<Any> {
        return methods
            .map { m ->
                val targetMethodInfo = if (!requireDescriptor && m.method.name in uniqueMethods) {
                    MemberReference(m.method.name)
                } else {
                    m.method.memberReference
                }

                val sourceMethod = m.method.findOrConstructSourceMethod(
                    m.clazz,
                    context.project,
                    scope = context.resolveScope,
                    canDecompile = false,
                )
                val builder = JavaLookupElementBuilder.forMethod(
                    sourceMethod,
                    targetMethodInfo.toMixinString(),
                    PsiSubstitutor.EMPTY,
                    null,
                )
                    .withPresentableText(m.method.name)
                addCompletionInfo(builder, context, targetMethodInfo)
            }.toTypedArray()
    }

    open val requireDescriptor = false

    open fun addCompletionInfo(
        builder: LookupElementBuilder,
        context: PsiElement,
        targetMethodInfo: MemberReference,
    ): LookupElementBuilder {
        return builder.completeToLiteral(context)
    }
}
