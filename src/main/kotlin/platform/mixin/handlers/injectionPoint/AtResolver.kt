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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarContext
import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.InjectionPointSpecifier
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.SHIFT
import com.demonwav.mcdev.platform.mixin.util.findSourceClass
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.isClinit
import com.demonwav.mcdev.platform.mixin.util.memberReference
import com.demonwav.mcdev.util.Quantifier
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.countIsAtLeast
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.internalName
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameterListOwner
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parents
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
    private val targetMethod: MethodNode,
) {
    companion object {
        fun getInjectionPoint(at: PsiAnnotation): InjectionPoint<*>? {
            var atCode = at.qualifiedName?.let { InjectionPointAnnotation.atCodeFor(it) }
                ?: at.findDeclaredAttributeValue("value")?.constantStringValue ?: return null

            // remove specifier
            if (InjectionPointSpecifier.isAllowed(at)) {
                if (InjectionPointSpecifier.entries.any { atCode.endsWith(":${it.name}") }) {
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
            val args = at.findAttributeValue("args")?.computeStringArray().orEmpty()
            val explicitArgs = args.associate {
                val parts = it.split('=', limit = 2)
                if (parts.size == 1) {
                    parts[0] to ""
                } else {
                    parts[0] to parts[1]
                }
            }
            return getInherentArgs(at) + explicitArgs
        }

        private fun getInherentArgs(at: PsiAnnotation): Map<String, String> {
            return at.attributes.asSequence()
                .mapNotNull {
                    val name = it.attributeName
                    val value = at.findAttributeValue(name) ?: return@mapNotNull null
                    val string = valueToString(value) ?: return@mapNotNull null
                    name to string
                }
                .toMap()
        }

        private fun valueToString(value: PsiAnnotationMemberValue): String? {
            if (value is PsiArrayInitializerMemberValue) {
                return value.initializers.map { valueToString(it) ?: return null }.joinToString(",")
            }
            return when (val constant = value.constantValue) {
                is PsiClassType -> constant.resolve()?.internalName
                is PsiType -> constant.descriptor
                null -> when (value) {
                    is PsiReferenceExpression -> value.referenceName
                    else -> null
                }
                else -> constant.toString()
            }
        }

        fun findInjectorAnnotation(at: PsiAnnotation, skipThroughSlice: Boolean = true): PsiAnnotation? {
            return at.parents(false)
                .takeWhile { it !is PsiClass }
                .filterIsInstance<PsiAnnotation>()
                .firstOrNull {
                    !skipThroughSlice || (!it.hasQualifiedName(SLICE) && !it.hasQualifiedName(AT))
                }
        }

        fun getShift(at: PsiAnnotation): Int {
            val shiftAttr = at.findDeclaredAttributeValue("shift") as? PsiExpression ?: return 0
            val shiftReference = PsiUtil.skipParenthesizedExprDown(shiftAttr) as? PsiReferenceExpression ?: return 0
            val shift = shiftReference.resolve() as? PsiEnumConstant ?: return 0
            val containingClass = shift.containingClass ?: return 0
            val shiftClass = JavaPsiFacade.getInstance(at.project).findClass(SHIFT, at.resolveScope) ?: return 0
            if (!(containingClass equivalentTo shiftClass)) return 0
            return when (shift.name) {
                "BEFORE" -> -1
                "AFTER" -> 1
                "BY" -> at.findDeclaredAttributeValue("by")?.constantValue as? Int ?: 0
                else -> 0
            }
        }

        const val DEFAULT_UNRESOLVED_MESSAGE = "Cannot resolve any instructions in target"
    }

    fun isUnresolved(): InsnResolutionInfo.Failure? {
        val injectionPoint = getInjectionPoint(at)
            ?: return null // we don't know what to do with custom handlers, assume ok

        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val collectVisitor = injectionPoint.createCollectVisitor(
            at,
            target,
            getTargetClass(target),
            CollectVisitor.Mode.RESOLUTION,
        )
        if (collectVisitor == null) {
            // syntax error in target
            val stringValue = targetAttr?.constantStringValue ?: return InsnResolutionInfo.Failure(DEFAULT_UNRESOLVED_MESSAGE)
            return if (isMiscDynamicSelector(at.project, stringValue)) {
                null
            } else {
                InsnResolutionInfo.Failure(DEFAULT_UNRESOLVED_MESSAGE)
            }
        }
        return when (val result = collectVisitor.visit(targetMethod)) {
            is InsnResolutionInfo.Failure -> result
            is InsnResolutionInfo.Success -> {
                val minMatches = collectVisitor.quantifier.min(Quantifier.Context.INSTRUCTION).coerceAtLeast(1)
                if (result.results.countIsAtLeast(minMatches)) {
                    null
                } else {
                    InsnResolutionInfo.Failure("Quantifier requires at least $minMatches matches")
                }
            }
        }
    }

    fun resolveInstructions(
        mode: CollectVisitor.Mode = CollectVisitor.Mode.RESOLUTION,
    ): Sequence<CollectVisitor.Result<*>> {
        return getInstructionResolutionInfo(mode).results
    }

    fun getInstructionResolutionInfo(mode: CollectVisitor.Mode = CollectVisitor.Mode.RESOLUTION): InsnResolutionInfo<*> {
        val injectionPoint = getInjectionPoint(at) ?: return InsnResolutionInfo.Failure(DEFAULT_UNRESOLVED_MESSAGE)
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }

        val collectVisitor = injectionPoint.createCollectVisitor(at, target, getTargetClass(target), mode)
            ?: return InsnResolutionInfo.Failure(DEFAULT_UNRESOLVED_MESSAGE)

        return collectVisitor.visit(targetMethod)
    }

    fun resolveNavigationTargets(): List<PsiElement> {
        // First resolve the actual target in the bytecode using the collect visitor
        val injectionPoint = getInjectionPoint(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val bytecodeResults = resolveInstructions()

        val project = at.project

        // Resolve the target source class
        val targetPsiClass = getTargetClass(target)
            .findSourceClass(project, GlobalSearchScope.allScope(project), canDecompile = true)
            ?: return emptyList()
        val targetPsiFile = targetPsiClass.containingFile ?: return emptyList()

        // Desugar the target class
        val desugaredTargetClass = DesugarUtil.desugar(project, targetPsiClass, DesugarContext(targetClass.version))
            ?: return emptyList()

        // Find the element in the desugared class, first by directly searching and then by searching in the original
        // and reverse mapping it into the desugared class.
        val desugaredTargetElement = when {
            targetMethod.isClinit -> desugaredTargetClass.initializers.firstOrNull {
                it.hasModifierProperty(PsiModifier.STATIC)
            }
            else -> desugaredTargetClass.findMethods(targetMethod.memberReference).firstOrNull()
        } ?: run {
            val originalTargetElement = targetMethod.findSourceElement(
                getTargetClass(target),
                project,
                GlobalSearchScope.allScope(project),
                canDecompile = true,
            ) ?: return emptyList()
            DesugarUtil.getOriginalToDesugaredMap(desugaredTargetClass)[originalTargetElement]
                ?.firstOrNull { it is PsiParameterListOwner }
                ?: return listOf(originalTargetElement)
        }

        // Find the source element in the desugared class
        val navigationVisitor = injectionPoint.createNavigationVisitor(at, target, targetPsiClass) ?: return emptyList()
        navigationVisitor.configureBytecodeTarget(targetClass, targetMethod)
        navigationVisitor.visitStart(desugaredTargetElement)
        if (desugaredTargetElement is PsiParameterListOwner) {
            desugaredTargetElement.acceptChildren(navigationVisitor)
        } else {
            desugaredTargetElement.accept(navigationVisitor)
        }
        navigationVisitor.visitEnd(desugaredTargetElement)

        // Map the desugared results back into the original source class
        val sourceResults = navigationVisitor.result.mapNotNull(DesugarUtil::getOriginalElement)

        // Match the bytecode results to the source results
        return bytecodeResults.mapNotNull { bytecodeResult ->
            val matcher = bytecodeResult.sourceLocationInfo.createMatcher<PsiElement>(targetPsiFile)
            sourceResults.forEach(matcher::accept)
            matcher.result
        }
            .toList()
    }

    fun collectTargetVariants(completionHandler: (LookupElementBuilder) -> LookupElementBuilder): List<Any> {
        val injectionPoint = getInjectionPoint(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val injector = findInjectorAnnotation(at, skipThroughSlice = false)?.let(MixinAnnotationHandler::forMixinAnnotation)
            as? InjectorAnnotationHandler

        // Collect all possible targets
        fun <T : PsiElement> doCollectVariants(injectionPoint: InjectionPoint<T>): List<Any> {
            val visitor = injectionPoint.createCollectVisitor(
                at, target, getTargetClass(target),
                CollectVisitor.Mode.COMPLETION
            )
                ?: return emptyList()

            return visitor.visit(targetMethod)
                .results
                .filter { result -> injector?.isInsnAllowed(result.insn, result.decorations) != false }
                .mapNotNull { result ->
                    injectionPoint.createLookup(getTargetClass(target), result)?.let { completionHandler(it) }
                }
                .toList()
        }
        return doCollectVariants(injectionPoint)
    }

    private fun getTargetClass(selector: MixinSelector?): ClassNode {
        return selector?.getCustomOwner(targetClass) ?: targetClass
    }
}

sealed class InsnResolutionInfo<out T : PsiElement>(val results: Sequence<CollectVisitor.Result<T>>) {
    class Success<T : PsiElement>(results: Sequence<CollectVisitor.Result<T>>) : InsnResolutionInfo<T>(results)
    class Failure(val messages: Set<String>, val filterStats: Map<String, Int>) : InsnResolutionInfo<Nothing>(emptySequence()) {
        constructor(message: String, filterStats: Map<String, Int> = emptyMap()) : this(linkedSetOf(message), filterStats)

        infix fun combine(other: Failure): Failure {
            val messages = linkedSetOf<String>()
            messages += this.messages
            messages += other.messages

            val result = LinkedHashMap(this.filterStats)
            for ((key, value) in other.filterStats) {
                result[key] = (result[key] ?: 0) + value
            }
            return Failure(messages, result)
        }
    }
}

object QualifiedMember {
    fun resolveQualifier(reference: PsiQualifiedReference): PsiClass? {
        val qualifier = reference.qualifier ?: return null
        ((qualifier as? PsiReference)?.resolve() as? PsiClass)?.let { return it }
        ((qualifier as? PsiExpression)?.type as? PsiClassType)?.resolve()?.let { return it }
        return null
    }
}
