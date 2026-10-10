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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MemberInfo
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.findUpstreamMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Quantifier
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.countIsAtLeast
import com.demonwav.mcdev.util.countIsLessThan
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.toResolveResults
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
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

        val allowStatic = context.parentOfType<PsiMethod>()?.hasModifierProperty(PsiModifier.STATIC) ?: true
        val stringValue = context.constantStringValue ?: return false
        val targetMethodInfo = parseSelector(stringValue, context) ?: return false
        val minMatches = generateSequence(targetMethodInfo) { it.next }
            .last()
            .quantifier
            .min(Quantifier.Context.MEMBER)
            .coerceAtLeast(1)
        val targets = getTargets(context) ?: return false

        return targets.any {
            targetMethodInfo.getCustomOwner(it).findMethods(listOf(targetMethodInfo), allowStatic)
                .countIsLessThan(minMatches)
        }
    }

    fun getReferenceIfAmbiguous(context: PsiElement): MemberInfo? {
        val targetReference = parseSelector(context) as? MemberInfo ?: return null
        if (targetReference.name != null && targetReference.descriptor != null) {
            // Not ambiguous
            return null
        }

        val targets = getTargets(context) ?: return null
        return if (isAmbiguous(targets, targetReference)) targetReference else null
    }

    private fun isAmbiguous(targets: Collection<ClassNode>, targetReference: MemberInfo): Boolean {
        val selector = targetReference.copy(quantifier = Quantifier.Any, next = null)
        return targets.any {
            it.findMethods(listOf(selector), allowStatic = true).countIsAtLeast(2)
        }
    }

    fun resolve(context: PsiElement): Sequence<ClassAndMethodNode>? {
        val allowStatic = context.parentOfType<PsiMethod>()?.hasModifierProperty(PsiModifier.STATIC) ?: true
        val targets = getTargets(context) ?: return null
        val targetedMethods = when (context) {
            is PsiArrayInitializerMemberValue -> context.initializers.mapNotNull { it.constantStringValue }
            else -> context.constantStringValue?.let { listOf(it) } ?: emptyList()
        }
        val selectors = targetedMethods.mapNotNull { parseSelector(it, context) }

        return resolve(targets, selectors, allowStatic)
    }

    private fun resolve(
        targets: Collection<ClassNode>,
        selectors: List<MixinSelector>,
        allowStatic: Boolean,
    ): Sequence<ClassAndMethodNode> {
        return targets.asSequence()
            .flatMap { target ->
                selectors.asSequence().map { it.getCustomOwner(target) to it }
            }
            .groupBy({ it.first }, { it.second })
            .asSequence()
            .flatMap { (target, selectors) ->
                target.findMethods(selectors, allowStatic)
                    .map { ClassAndMethodNode(target, it) }
            }
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

    final override fun collectVariants(context: PsiElement): Array<Any> {
        val targets = getTargets(context) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return getSuggestions(context, targets)
    }

    abstract fun getSuggestions(context: PsiElement, targets: Collection<ClassNode>): Array<Any>

    protected fun methodLookupBuilder(
        m: ClassAndMethodNode,
        targetMethodInfo: MemberReference,
        context: PsiElement,
    ): LookupElementBuilder {
        val sourceMethod = m.method.findOrConstructSourceMethod(
            m.clazz,
            context.project,
            scope = context.resolveScope,
            canDecompile = false,
        )
        return JavaLookupElementBuilder.forMethod(
            sourceMethod,
            targetMethodInfo.toMixinString(),
            PsiSubstitutor.EMPTY,
            null,
        )
            .withPresentableText(m.method.name)
    }
}
