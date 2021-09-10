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
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class InjectionPoint<T : PsiElement> {
    companion object {
        private val COLLECTOR =
            KeyedExtensionCollector<InjectionPoint<*>, String>("com.demonwav.minecraft-dev.injectionPoint")

        fun byAtCode(atCode: String): InjectionPoint<*>? {
            return COLLECTOR.findSingle(atCode)
        }
    }

    open fun usesMemberReference() = false

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

class InjectionPointInfo : BaseKeyedLazyInstance<InjectionPoint<*>>(), KeyedLazyInstance<InjectionPoint<*>> {
    @Attribute("atCode")
    @RequiredElement
    lateinit var atCode: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getImplementationClassName(): String {
        return implementation
    }

    override fun getKey(): String {
        return atCode
    }
}

abstract class QualifiedInjectionPoint<T : PsiMember> : InjectionPoint<T>() {

    final override fun usesMemberReference() = true

    protected abstract fun createLookup(targetClass: ClassNode, m: T, owner: String): LookupElementBuilder

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

abstract class AbstractMethodInjectionPoint : QualifiedInjectionPoint<PsiMethod>() {

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

abstract class CollectVisitor<T : PsiElement>(protected val mode: Mode) {
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

    data class Result<T : PsiElement>(
        val index: Int,
        val insn: AbstractInsnNode,
        val target: T,
        val qualifier: String? = null
    )

    enum class Mode { MATCH_ALL, MATCH_FIRST, COMPLETION }
}
