/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
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
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import org.objectweb.asm.tree.ClassNode

/**
 * The reference inside e.g. @Inject.method(). Similar to [TargetReference], this reference has different ways of being
 * resolved. See the docs for that class for details.
 */
object MethodReference : PolyReferenceResolver(), MixinReference {

    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationParam(
            StandardPatterns.string().oneOf(METHOD_INJECTORS),
            "method"
        )

    override val description: String
        get() = "method '%s' in target class"

    override fun isValidAnnotation(name: String) = name in METHOD_INJECTORS

    private fun getTargets(context: PsiElement): Collection<ClassNode>? {
        val psiClass = context.findContainingClass() ?: return null
        val targets = psiClass.mixinTargets
        val upstreamMixin = context.findContainingMethod()?.findUpstreamMixin()?.bytecode
        return when {
            upstreamMixin != null -> targets + upstreamMixin
            else -> targets
        }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val stringValue = context.constantStringValue ?: return false
        val targetMethodInfo = parseMixinSelector(stringValue, context) ?: return false
        val targets = getTargets(context) ?: return false
        if (targets.asSequence().flatMap { it.findMethods(targetMethodInfo) }.any()) {
            return false
        }
        return !isDynamicSelector(context.project, stringValue)
    }

    fun getReferenceIfAmbiguous(context: PsiElement): MemberReference? {
        val targetReference = parseMixinSelector(context) as? MemberReference ?: return null
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
            is PsiLiteral -> context.constantStringValue?.let { listOf(it) } ?: emptyList()
            is PsiArrayInitializerMemberValue -> context.initializers.mapNotNull { it.constantStringValue }
            else -> emptyList()
        }

        return targetedMethods.asSequence().flatMap { method ->
            val targetReference = parseMixinSelector(method, context) ?: return@flatMap emptySequence()
            return@flatMap resolve(targets, targetReference)
        }
    }

    private fun resolve(
        targets: Collection<ClassNode>,
        selector: MixinSelector
    ): Sequence<ClassAndMethodNode> {
        return targets.asSequence()
            .flatMap { target -> target.findMethods(selector).map { ClassAndMethodNode(target, it) } }
    }

    fun resolveIfUnique(context: PsiElement): ClassAndMethodNode? {
        return resolve(context)?.singleOrNull()
    }

    fun resolveAllIfNotAmbiguous(context: PsiElement): List<ClassAndMethodNode>? {
        val targets = getTargets(context) ?: return null

        val targetedMethods = when (context) {
            is PsiLiteral -> context.constantStringValue?.let { listOf(it) } ?: emptyList()
            is PsiArrayInitializerMemberValue -> context.initializers.mapNotNull { it.constantStringValue }
            else -> emptyList()
        }

        return targetedMethods.asSequence().flatMap { method ->
            val targetReference = parseMixinSelector(method, context) ?: return@flatMap emptySequence()
            if (targetReference is MemberReference && targetReference.descriptor == null && isAmbiguous(
                    targets,
                    targetReference
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
                canDecompile = true
            )
        }?.toTypedArray()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        return resolve(context)?.mapNotNull {
            it.method.findSourceElement(
                it.clazz,
                context.project,
                scope = context.resolveScope,
                canDecompile = false
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
        uniqueMethods: Set<String>
    ): Array<Any> {
        return methods
            .map { m ->
                val targetMethodInfo = if (m.method.name in uniqueMethods) {
                    MemberReference(m.method.name)
                } else {
                    m.method.memberReference
                }

                val sourceMethod = m.method.findOrConstructSourceMethod(
                    m.clazz,
                    context.project,
                    scope = context.resolveScope,
                    canDecompile = false
                )
                JavaLookupElementBuilder.forMethod(
                    sourceMethod,
                    targetMethodInfo.toMixinString(),
                    PsiSubstitutor.EMPTY,
                    null
                )
                    .withPresentableText(m.method.name)
                    .completeToLiteral(context)
            }.toTypedArray()
    }
}
