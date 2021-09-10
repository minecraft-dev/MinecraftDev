/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * Resolves targets of @At.
 *
 * Resolution of this reference depends on @At.value(), each of which have their own [Handler]. This handler is in
 * charge of parsing, validating and resolving this reference.
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
 * To support the above, handlers must be able to resolve the target element, and support a collect visitor and a
 * navigation visitor. The collect visitor finds target instructions in the bytecode of the target method, and the
 * navigation visitor makes a best-effort attempt at matching source code elements.
 */
class AtResolver(
    private val at: PsiAnnotation,
    private val targetClass: ClassNode,
    private val targetMethod: MethodNode
) {
    companion object {
        private fun getHandler(at: PsiAnnotation): Handler<*>? {
            val injectionPointType = at.findDeclaredAttributeValue("value")?.constantStringValue ?: return null
            return when (injectionPointType) {
                "INVOKE", "INVOKE_ASSIGN" -> MethodTargetReference
                "INVOKE_STRING" -> ConstantStringMethodTargetReference
                "FIELD" -> FieldTargetReference
                "NEW" -> NewInsnTargetReference

                else -> null // Unsupported injection point type
            }
        }

        fun usesMemberReference(at: PsiAnnotation): Boolean {
            val handler = getHandler(at) ?: return false
            return handler.usesMemberReference()
        }
    }

    fun isUnresolved(): Boolean {
        val handler = getHandler(at) ?: return false // we don't know what to do with custom handlers, assume ok

        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }
        val collectVisitor = handler.createCollectVisitor(at, target, targetClass, CollectVisitor.Mode.MATCH_FIRST)
        if (collectVisitor == null) {
            // syntax error in target
            val stringValue = targetAttr?.constantStringValue ?: return true
            return !isMiscDynamicSelector(at.project, stringValue)
        }
        collectVisitor.visit(targetMethod)
        return collectVisitor.result.isEmpty()
    }

    fun resolveInstructions(): List<CollectVisitor.Result<*>> {
        val handler = getHandler(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }

        val collectVisitor = handler.createCollectVisitor(at, target, targetClass, CollectVisitor.Mode.MATCH_ALL)
            ?: return emptyList()
        collectVisitor.visit(targetMethod)
        return collectVisitor.result
    }

    fun resolveNavigationTargets(): List<PsiElement> {
        // First resolve the actual target in the bytecode using the collect visitor
        val handler = getHandler(at) ?: return emptyList()
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

        val navigationVisitor = handler.createNavigationVisitor(at, target, targetPsiClass) ?: return emptyList()
        targetElement.accept(navigationVisitor)

        return bytecodeResults.mapNotNull { bytecodeResult ->
            navigationVisitor.result.getOrNull(bytecodeResult.index)
        }
    }

    fun collectTargetVariants(completionHandler: (LookupElementBuilder) -> LookupElementBuilder): List<Any> {
        val handler = getHandler(at) ?: return emptyList()
        val targetAttr = at.findAttributeValue("target")
        val target = targetAttr?.let { parseMixinSelector(it) }

        // Collect all possible targets
        fun <T : PsiMember> doCollectVariants(handler: Handler<T>): List<Any> {
            val visitor = handler.createCollectVisitor(at, target, targetClass, CollectVisitor.Mode.COMPLETION)
                ?: return emptyList()
            visitor.visit(targetMethod)
            return visitor.result
                .mapNotNull { result ->
                    handler.createLookup(targetClass, result)?.let { completionHandler(it) }
                }
        }
        return doCollectVariants(handler)
    }

    abstract class Handler<T : PsiMember> {

        open fun usesMemberReference() = false

        abstract fun resolveTarget(context: PsiElement): PsiElement?

        abstract fun createNavigationVisitor(
            at: PsiAnnotation,
            target: MixinSelector?,
            targetClass: PsiClass
        ): NavigationVisitor?

        abstract fun createCollectVisitor(
            at: PsiAnnotation,
            target: MixinSelector?,
            targetClass: ClassNode,
            mode: CollectVisitor.Mode
        ): CollectVisitor<T>?

        abstract fun createLookup(targetClass: ClassNode, result: CollectVisitor.Result<T>): LookupElementBuilder?

        protected fun LookupElementBuilder.setBoldIfInClass(member: PsiMember, clazz: ClassNode): LookupElementBuilder {
            if (member.containingClass?.fullQualifiedName?.replace('.', '/') == clazz.name) {
                return bold()
            }
            return this
        }
    }

    abstract class QualifiedHandler<T : PsiMember> : Handler<T>() {

        final override fun usesMemberReference() = true

        protected abstract fun createLookup(targetClass: ClassNode, m: T, owner: String): LookupElementBuilder

        override fun resolveTarget(context: PsiElement): PsiElement? {
            val selector = parseMixinSelector(context)
            selector?.owner ?: return null
            return selector.resolveMember(context.project, context.resolveScope)
        }

        protected open fun getInternalName(m: T): String {
            return m.realName ?: m.name!!
        }

        final override fun createLookup(
            targetClass: ClassNode,
            result: CollectVisitor.Result<T>
        ): LookupElementBuilder {
            return qualifyLookup(
                createLookup(targetClass, result.target, result.qualifier ?: targetClass.name),
                targetClass,
                result.target
            )
        }

        private fun qualifyLookup(
            builder: LookupElementBuilder,
            targetClass: ClassNode,
            m: T
        ): LookupElementBuilder {
            val owner = m.containingClass!!
            return if (targetClass.name == owner.fullQualifiedName?.replace('.', '/')) {
                builder
            } else {
                // Qualify member with name of owning class
                builder.withPresentableText(owner.shortName + '.' + getInternalName(m))
            }
        }
    }

    abstract class MethodHandler : QualifiedHandler<PsiMethod>() {

        override fun createLookup(targetClass: ClassNode, m: PsiMethod, owner: String): LookupElementBuilder {
            return JavaLookupElementBuilder.forMethod(
                m,
                m.getQualifiedMemberReference(owner).toMixinString(),
                PsiSubstitutor.EMPTY,
                null
            )
                .setBoldIfInClass(m, targetClass)
                .withPresentableText(m.internalName) // Display internal name (e.g. <init> for constructors)
                .withLookupString(m.internalName) // Allow looking up targets by their method name
        }

        override fun getInternalName(m: PsiMethod): String {
            return m.internalName
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

abstract class NavigationVisitor : JavaRecursiveElementVisitor() {
    val result = mutableListOf<PsiElement>()
    private var hasVisitedAnything = false

    protected fun addResult(element: PsiElement) {
        result += element
    }

    override fun visitElement(element: PsiElement) {
        hasVisitedAnything = true
        super.visitElement(element)
    }

    override fun visitAnonymousClass(aClass: PsiAnonymousClass?) {
        // do not recurse into anonymous classes
        if (!hasVisitedAnything) {
            super.visitAnonymousClass(aClass)
        }
    }

    override fun visitClass(aClass: PsiClass?) {
        // do not recurse into inner classes
        if (!hasVisitedAnything) {
            super.visitClass(aClass)
        }
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression?) {
        // do not recurse into lambda expressions
        if (!hasVisitedAnything) {
            super.visitLambdaExpression(expression)
        }
    }
}

abstract class CollectVisitor<T : PsiMember>(protected val mode: Mode) {
    fun visit(methodNode: MethodNode) {
        try {
            accept(methodNode)
        } catch (e: StopWalkingException) {
            // ignore
        }
    }

    protected abstract fun accept(methodNode: MethodNode)

    private var nextIndex = 0
    val result = mutableListOf<Result<T>>()

    protected fun addResult(insn: AbstractInsnNode, element: T, qualifier: String? = null) {
        this.result.add(Result(nextIndex++, insn, element, qualifier))
        if (mode == Mode.MATCH_FIRST) {
            stopWalking()
        }
    }

    protected fun stopWalking() {
        throw StopWalkingException()
    }

    private class StopWalkingException : Exception() {
        override fun fillInStackTrace(): Throwable {
            return this
        }
    }

    data class Result<T : PsiMember>(
        val index: Int,
        val insn: AbstractInsnNode,
        val target: T,
        val qualifier: String? = null
    )

    enum class Mode { MATCH_ALL, MATCH_FIRST, COMPLETION }
}
