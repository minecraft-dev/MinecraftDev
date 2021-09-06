/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.util.annotationFromArrayValue
import com.demonwav.mcdev.util.annotationFromValue
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.notNullToArray
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.shortName
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

/**
 * The reference inside @At.target().
 *
 * Resolution of this reference depends on @At.value(), each of which have their own [Handler]. This handler is in
 * charge of parsing, validating and resolving this reference.
 *
 * This reference can be resolved in four different ways.
 * - [isUnresolved] only checks the bytecode of the target class, to check whether this reference is valid.
 * - [resolveReference] resolves to the actual member being targeted, rather than the location it's referenced in the
 *   target method. This serves as a backup in case nothing else is found to navigate to, and so that find usages can
 *   take you back to this reference.
 * - [collectVariants] is used for auto-completion. It does not take into account what is actually in the target string,
 *   and instead matches everything the handler *could* match. The references resolve similarly to `resolveReference`,
 *   although new elements may be created if not found.
 * - [resolveNavigationTargets] is used when the user attempts to navigate on this reference. This attempts to take you
 *   to the actual location in the source code of the target class which is being targeted. Potentially slow as it may
 *   decompile the target class.
 *
 * To support the above, handlers must be able to resolve the target element, and support a collect visitor and a
 * navigation visitor. The collect visitor finds target instructions in the bytecode of the target method, and the
 * navigation visitor makes a best-effort attempt at matching source code elements.
 */
object TargetReference : PolyReferenceResolver(), MixinReference {

    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(AT, "target")

    override val description: String
        get() = "target reference '%s'"

    override fun isValidAnnotation(name: String) = name == AT

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

    fun usesMemberReference(context: PsiElement): Boolean {
        val handler = getHandler(context.annotationFromArrayValue!!) ?: return false
        return handler.usesMemberReference()
    }

    fun resolveTarget(context: PsiElement): PsiElement? {
        val handler = getHandler(context.annotationFromValue ?: return null) ?: return null
        return handler.resolveTarget(context)
    }

    private fun getTargetMethod(at: PsiAnnotation): ClassAndMethodNode? {
        // TODO: Right now this will only work for Mixins with a single target class
        val parentAnnotation = at.annotationFromArrayValue ?: return null
        val injectorAnnotation = if (parentAnnotation.qualifiedName == SLICE) {
            parentAnnotation.annotationFromValue ?: return null
        } else {
            parentAnnotation
        }

        val methodValue = injectorAnnotation.findDeclaredAttributeValue("method") ?: return null
        return MethodReference.resolveIfUnique(methodValue)
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val at = context.parentOfType<PsiAnnotation>() ?: return true // @At

        val handler = getHandler(at) ?: return false // we don't know what to do with custom handlers, assume ok

        val targetMethod = getTargetMethod(at) ?: return false // the target method inspection will catch this

        val collectVisitor = handler.createCollectVisitor(context, targetMethod.clazz, CollectVisitor.Mode.MATCH_FIRST)
            ?: return true // syntax error in target
        collectVisitor.visit(targetMethod.method)
        return collectVisitor.result.isEmpty()
    }

    fun resolveNavigationTargets(context: PsiElement): Array<PsiElement>? {
        // First resolve the actual target in the bytecode using the collect visitor
        val at = context.parentOfType<PsiAnnotation>() ?: return null // @At
        val handler = getHandler(at) ?: return null

        val targetMethod = getTargetMethod(at) ?: return null

        val collectVisitor = handler.createCollectVisitor(context, targetMethod.clazz, CollectVisitor.Mode.MATCH_ALL)
            ?: return null
        collectVisitor.visit(targetMethod.method)
        val bytecodeResults = collectVisitor.result

        // Then attempt to find the corresponding source elements using the navigation visitor
        val targetElement = targetMethod.method.findSourceElement(
            targetMethod.clazz,
            context.project,
            GlobalSearchScope.allScope(context.project),
            canDecompile = true
        ) ?: return null
        val targetPsiClass = targetElement.parentOfType<PsiClass>() ?: return null

        val navigationVisitor = handler.createNavigationVisitor(context, targetPsiClass) ?: return null
        targetElement.accept(navigationVisitor)

        return bytecodeResults.asSequence().mapNotNull { bytecodeResult ->
            navigationVisitor.result.getOrNull(bytecodeResult.index)
        }.toTypedArray()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val result = resolveTarget(context) ?: return ResolveResult.EMPTY_ARRAY
        return arrayOf(PsiElementResolveResult(result))
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val at = context.annotationFromValue ?: return ArrayUtil.EMPTY_OBJECT_ARRAY // @At
        val handler = getHandler(at) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val targetMethod = getTargetMethod(at) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        // Collect all possible targets
        fun <T : PsiMember> doCollectVariants(handler: Handler<T>): Array<Any> {
            val visitor = handler.createCollectVisitor(context, targetMethod.clazz, CollectVisitor.Mode.COMPLETION)
                ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
            visitor.visit(targetMethod.method)
            return visitor.result.asSequence()
                .map { handler.createLookup(targetMethod.clazz, it)?.completeToLiteral(context) }
                .notNullToArray()
        }
        return doCollectVariants(handler)
    }

    abstract class Handler<T : PsiMember> {

        open fun usesMemberReference() = false

        abstract fun resolveTarget(context: PsiElement): PsiElement?

        abstract fun createNavigationVisitor(
            context: PsiElement,
            targetClass: PsiClass
        ): NavigationVisitor?

        abstract fun createCollectVisitor(
            context: PsiElement,
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
            val value = context.constantStringValue ?: return null
            val selector = parseMixinSelector(value)
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

    protected fun addResult(element: T, qualifier: String? = null) {
        this.result.add(Result(nextIndex++, element, qualifier))
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

    data class Result<T : PsiMember>(val index: Int, val target: T, val qualifier: String? = null)

    enum class Mode { MATCH_ALL, MATCH_FIRST, COMPLETION }
}
