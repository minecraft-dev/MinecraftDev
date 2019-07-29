/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.platform.mixin.util.findSource
import com.demonwav.mcdev.util.annotationFromArrayValue
import com.demonwav.mcdev.util.annotationFromValue
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.notNullToArray
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil

object TargetReference : PolyReferenceResolver(), MixinReference {

    override val description: String
        get() = "target reference '%s'"

    override fun isValidAnnotation(name: String) = name == AT

    private fun getHandler(at: PsiAnnotation): Handler<*>? {
        val injectionPointType = at.findDeclaredAttributeValue("value")?.constantStringValue ?: return null
        return when (injectionPointType) {
            "INVOKE", "INVOKE_ASSIGN" -> MethodTargetReference
            "INVOKE_STRING" -> ConstantStringMethodTargetReference
            "FIELD" -> FieldTargetReference
            "NEW" -> ConstructorTargetReference

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

    private fun getTargetMethod(at: PsiAnnotation): PsiMethod? {
        // TODO: Right now this will only work for Mixins with a single target class
        val parentAnnotation = at.annotationFromArrayValue ?: return null
        val injectorAnnotation = if (parentAnnotation.qualifiedName == SLICE) {
            parentAnnotation.annotationFromValue ?: return null
        } else {
            parentAnnotation
        }

        val methodValue = injectorAnnotation.findDeclaredAttributeValue("method") ?: return null
        return MethodReference.resolveIfUnique(methodValue)?.findSource()
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val result = resolve(context, checkOnly = true) ?: return false
        return result.isEmpty()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val result = resolve(context, checkOnly = false) ?: return ResolveResult.EMPTY_ARRAY
        return result.mapToArray(::PsiElementResolveResult)
    }

    private fun resolve(context: PsiElement, checkOnly: Boolean): List<PsiElement>? {
        val at = context.annotationFromValue!! // @At
        val handler = getHandler(at) ?: return null

        val targetMethod = getTargetMethod(at) ?: return null
        val codeBlock = targetMethod.body ?: return null

        val visitor = handler.createFindUsagesVisitor(context, targetMethod.containingClass!!, checkOnly) ?: return null
        codeBlock.accept(visitor)
        return visitor.result
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val at = context.annotationFromValue ?: return ArrayUtil.EMPTY_OBJECT_ARRAY // @At
        val handler = getHandler(at) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val targetMethod = getTargetMethod(at) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val codeBlock = targetMethod.body ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val containingClass = targetMethod.containingClass ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return collectUsages(context, handler, codeBlock, containingClass)
    }

    private fun <T> collectUsages(
        context: PsiElement,
        handler: Handler<T>,
        codeBlock: PsiElement,
        targetClass: PsiClass
    ): Array<Any> {
        // Collect all possible targets
        val visitor = handler.createCollectUsagesVisitor()
        codeBlock.accept(visitor)
        return visitor.result.asSequence()
            .map { handler.createLookup(targetClass, it)?.completeToLiteral(context) }
            .notNullToArray()
    }

    abstract class Handler<T> {

        open fun usesMemberReference() = false

        abstract fun resolveTarget(context: PsiElement): PsiElement?

        abstract fun createFindUsagesVisitor(
            context: PsiElement,
            targetClass: PsiClass,
            checkOnly: Boolean
        ): CollectVisitor<out PsiElement>?

        abstract fun createCollectUsagesVisitor(): CollectVisitor<T>

        abstract fun createLookup(targetClass: PsiClass, element: T): LookupElementBuilder?
    }

    abstract class QualifiedHandler<T : PsiMember> : Handler<QualifiedMember<T>>() {

        final override fun usesMemberReference() = true

        protected abstract fun createLookup(targetClass: PsiClass, m: T, owner: PsiClass): LookupElementBuilder

        override fun resolveTarget(context: PsiElement): PsiElement? {
            val value = context.constantStringValue ?: return null
            val ref = MixinMemberReference.parse(value)
            ref?.owner ?: return null
            return ref.resolveMember(context.project, context.resolveScope)
        }

        protected open fun getInternalName(m: QualifiedMember<T>): String {
            return m.member.name!!
        }

        final override fun createLookup(targetClass: PsiClass, element: QualifiedMember<T>): LookupElementBuilder? {
            return qualifyLookup(
                createLookup(targetClass, element.member, element.qualifier ?: targetClass),
                targetClass,
                element
            )
        }

        private fun qualifyLookup(
            builder: LookupElementBuilder,
            targetClass: PsiClass,
            m: QualifiedMember<T>
        ): LookupElementBuilder {
            val owner = m.member.containingClass!!
            return if (targetClass equivalentTo owner) {
                builder
            } else {
                // Qualify member with name of owning class
                builder.withPresentableText(owner.shortName + '.' + getInternalName(m))
            }
        }
    }

    abstract class MethodHandler : QualifiedHandler<PsiMethod>() {

        override fun createLookup(targetClass: PsiClass, m: PsiMethod, owner: PsiClass): LookupElementBuilder {
            return JavaLookupElementBuilder.forMethod(
                m, MixinMemberReference.toString(m.getQualifiedMemberReference(owner)),
                PsiSubstitutor.EMPTY, targetClass
            )
                .withPresentableText(m.internalName) // Display internal name (e.g. <init> for constructors)
                .withLookupString(m.internalName) // Allow looking up targets by their method name
        }

        override fun getInternalName(m: QualifiedMember<PsiMethod>): String {
            return m.member.internalName
        }
    }
}

data class QualifiedMember<T : PsiMember>(val member: T, val qualifier: PsiClass?) {
    constructor(member: T, reference: PsiQualifiedReference) : this(member, resolveQualifier(reference))

    companion object {

        fun resolveQualifier(reference: PsiQualifiedReference): PsiClass? {
            val qualifier = reference.qualifier ?: return null
            if (qualifier is PsiThisExpression) {
                return null
            }

            ((qualifier as? PsiReference)?.resolve() as? PsiClass)?.let { return it }
            ((qualifier as? PsiExpression)?.type as? PsiClassType)?.resolve()?.let { return it }
            return null
        }
    }
}

abstract class CollectVisitor<T>(private val checkOnly: Boolean) : JavaRecursiveElementWalkingVisitor() {

    val result = ArrayList<T>()

    protected fun addResult(element: T) {
        this.result.add(element)
        if (checkOnly) {
            stopWalking()
        }
    }
}
