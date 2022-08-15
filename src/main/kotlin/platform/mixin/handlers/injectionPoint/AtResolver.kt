/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * Resolves targets of @At.
 *
 * Resolution of this reference depends on @At.value(), each of which have their own [InjectionPoint]. This injection
 * point is in charge of parsing, validating and resolving this reference.
 *
 * This reference can be resolved in four different ways.
 * - [isUnresolved] only checks the bytecode of the target class, to check whether this reference is valid.
 * - [TargetReference.resolveReference] resolves to the actual member being targeted, rather than the location it's
 *   referenced in the target method. This serves as a backup in case nothing else is found to navigate to, and so that
 *   find usages can take you back to this reference.
 * - [collectTargetVariants] is used for auto-completion. It does not take into account what is actually in the target
 *   string, and instead matches everything the handler *could* match. The references resolve similarly to
 *   `resolveReference`, although new elements may be created if not found.
 * - [resolveNavigationTargets] is used when the user attempts to navigate on this reference. This attempts to take you
 *   to the actual location in the source code of the target class which is being targeted. Potentially slow as it may
 *   decompile the target class.
 *
 * To support the above, injection points must be able to resolve the target element, and support a collect visitor and
 * a navigation visitor. The collect visitor finds target instructions in the bytecode of the target method, and the
 * navigation visitor makes a best-effort attempt at matching source code elements.
 */
class AtResolver(
    private val at: PsiAnnotation,
    private val targetClass: ClassNode,
    private val targetMethod: MethodNode
) {
    companion object {
        private fun getInjectionPoint(at: PsiAnnotation): InjectionPoint<*>? {
            var atCode = at.findDeclaredAttributeValue("value")?.constantStringValue ?: return null

            // remove slice selector
            val isInSlice = at.parentOfType<PsiAnnotation>()?.hasQualifiedName(SLICE) ?: false
            if (isInSlice) {
                if (SliceSelector.values().any { atCode.endsWith(":${it.name}") }) {
                    atCode = atCode.substringBeforeLast(':')
                }
            }

            return InjectionPoint.byAtCode(atCode)
        }

        fun usesMemberReference(at: PsiAnnotation): Boolean {
            val handler = getInjectionPoint(at) ?: return false
            return handler.usesMemberReference()
        }

        fun getArgs(at: PsiAnnotation): Map<String, String> {
            val args = at.findAttributeValue("args")?.computeStringArray() ?: return emptyMap()
            return args.asSequence()
                .map {
                    val parts = it.split('=', limit = 2)
                    if (parts.size == 1) {
                        parts[0] to ""
                    } else {
                        parts[0] to parts[1]
                    }
                }
                .toMap()
        }
    }

    fun isUnresolved(): InsnResolutionInfo.Failure? {
        val injectionPoint = getInjectionPoint(at)
            ?: return null // we don't know what to do with custom handlers, assume ok

        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val collectVisitor = injectionPoint.createCollectVisitor(
            at,
            target,
            targetClass,
            CollectVisitor.Mode.MATCH_FIRST
        )
        if (collectVisitor == null) {
            // syntax error in target
            val stringValue = targetAttr?.constantStringValue ?: return InsnResolutionInfo.Failure()
            return if (isMiscDynamicSelector(at.project, stringValue)) {
                null
            } else {
                InsnResolutionInfo.Failure()
            }
        }
        collectVisitor.visit(targetMethod)
        return if (collectVisitor.result.isEmpty()) {
            InsnResolutionInfo.Failure(collectVisitor.filterToBlame)
        } else {
            null
        }
    }

    fun resolveInstructions(mode: CollectVisitor.Mode = CollectVisitor.Mode.MATCH_ALL): List<CollectVisitor.Result<*>> {
        return (getInstructionResolutionInfo(mode) as? InsnResolutionInfo.Success)?.results ?: emptyList()
    }

    fun getInstructionResolutionInfo(mode: CollectVisitor.Mode = CollectVisitor.Mode.MATCH_ALL): InsnResolutionInfo {
        val injectionPoint = getInjectionPoint(at) ?: return InsnResolutionInfo.Failure()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }

        val collectVisitor = injectionPoint.createCollectVisitor(at, target, targetClass, mode)
            ?: return InsnResolutionInfo.Failure()
        collectVisitor.visit(targetMethod)
        val result = collectVisitor.result
        return if (result.isEmpty()) {
            InsnResolutionInfo.Failure(collectVisitor.filterToBlame)
        } else {
            InsnResolutionInfo.Success(result)
        }
    }

    fun resolveNavigationTargets(): List<PsiElement> {
        // First resolve the actual target in the bytecode using the collect visitor
        val injectionPoint = getInjectionPoint(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val bytecodeResults = resolveInstructions()

        // Then attempt to find the corresponding source elements using the navigation visitor
        val targetElement = targetMethod.findSourceElement(
            targetClass,
            at.project,
            GlobalSearchScope.allScope(at.project),
            canDecompile = true
        ) ?: return emptyList()
        val targetPsiClass = targetElement.parentOfType<PsiClass>() ?: return emptyList()

        val navigationVisitor = injectionPoint.createNavigationVisitor(at, target, targetPsiClass) ?: return emptyList()
        targetElement.accept(navigationVisitor)

        return bytecodeResults.mapNotNull { bytecodeResult ->
            navigationVisitor.result.getOrNull(bytecodeResult.index)
        }
    }

    fun collectTargetVariants(completionHandler: (LookupElementBuilder) -> LookupElementBuilder): List<Any> {
        val injectionPoint = getInjectionPoint(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }

        // Collect all possible targets
        fun <T : PsiElement> doCollectVariants(injectionPoint: InjectionPoint<T>): List<Any> {
            val visitor = injectionPoint.createCollectVisitor(at, target, targetClass, CollectVisitor.Mode.COMPLETION)
                ?: return emptyList()
            visitor.visit(targetMethod)
            return visitor.result
                .mapNotNull { result ->
                    injectionPoint.createLookup(targetClass, result)?.let { completionHandler(it) }
                }
        }
        return doCollectVariants(injectionPoint)
    }
}

sealed class InsnResolutionInfo {
    class Success(val results: List<CollectVisitor.Result<*>>) : InsnResolutionInfo()
    class Failure(val filterToBlame: String? = null) : InsnResolutionInfo() {
        infix fun combine(other: Failure): Failure {
            return if (filterToBlame != null) {
                this
            } else {
                other
            }
        }
    }
}

enum class SliceSelector {
    FIRST, LAST, ONE
}

object QualifiedMember {
    fun resolveQualifier(reference: PsiQualifiedReference): PsiClass? {
        val qualifier = reference.qualifier ?: return null
        ((qualifier as? PsiReference)?.resolve() as? PsiClass)?.let { return it }
        ((qualifier as? PsiExpression)?.type as? PsiClassType)?.resolve()?.let { return it }
        return null
    }
}
