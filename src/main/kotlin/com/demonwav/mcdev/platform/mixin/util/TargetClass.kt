/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DYNAMIC
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findMatchingMethods
import com.demonwav.mcdev.util.isMatchingField
import com.demonwav.mcdev.util.isMatchingMethod
import com.demonwav.mcdev.util.memberReference
import com.demonwav.mcdev.util.resolveClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.Contract

fun PsiMember.findUpstreamMixin(): PsiClass? {
    return findAnnotation(DYNAMIC)?.findDeclaredAttributeValue("mixin")?.resolveClass()
}

@get:Contract(pure = true)
val PsiElement.isWithinDynamicMixin: Boolean
    get() = findContainingMethod()?.findAnnotation(DYNAMIC) != null

fun findMethods(psiClass: PsiClass): Sequence<PsiMethod>? {
    val targets = psiClass.mixinTargets
    return when (targets.size) {
        0 -> null
        1 -> targets.single().methods.asSequence()
            .filter({ !it.isConstructor })
        else -> targets.asSequence()
            .flatMap { target -> target.methods.asSequence() }
            .filter({ !it.isConstructor })
            .groupBy { it.memberReference }
            .values.asSequence()
            .filter { it.size >= targets.size }
            .map { it.first() }
    }?.filter { m ->
        // Filter methods which are already in the Mixin class
        psiClass.findMatchingMethods(m, false).isEmpty()
    }
}

fun findFields(psiClass: PsiClass): Sequence<PsiField>? {
    val targets = psiClass.mixinTargets
    return when (targets.size) {
        0 -> null
        1 -> targets.single().fields.asSequence()
        else -> targets.asSequence()
            .flatMap { target -> target.fields.asSequence() }
            .groupBy { it.memberReference }
            .values.asSequence()
            .filter { it.size >= targets.size }
            .map { it.first() }
    }?.filter {
        // Filter fields which are already in the Mixin class
        psiClass.findFieldByName(it.name, false) == null
    }
}

fun findShadowTargets(psiClass: PsiClass, start: PsiClass, superMixin: Boolean): Sequence<ShadowTarget> {
    return if (superMixin) {
        findShadowTargetsDeep(psiClass, start)
    } else {
        // No need to walk the hierarchy if we don't have a super mixin
        findMethods(start).plus<PsiMember>(findFields(start))?.map { ShadowTarget(null, it) } ?: emptySequence()
    }
}

private fun findShadowTargetsDeep(psiClass: PsiClass, start: PsiClass): Sequence<ShadowTarget> {
    return start.streamMixinHierarchy()
        .flatMap { mixin ->
            findMethods(mixin).plus<PsiMember>(findFields(mixin))
                ?.filterAccessible(psiClass, mixin)
                ?.map { ShadowTarget(mixin.takeIf { !(it equivalentTo psiClass) }, it) } ?: emptySequence()
        }
        .distinctBy {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            when (it.member) {
                is PsiMethod -> MethodSignature(it.member)
                is PsiField -> FieldSignature(it.member)
                else -> throw AssertionError()
            }
        }
}

fun PsiMethod.findSource(): PsiMethod {
    val body = body
    if (body != null) {
        return this
    }

    // Attempt to find the source if we have a compiled method
    return (this as? ClsMethodImpl)?.sourceMirrorMethod ?: this
}

data class ShadowTarget(val mixin: PsiClass?, val member: PsiMember)

private fun <T : PsiMember> Sequence<T>.filterAccessible(psiClass: PsiClass, target: PsiClass): Sequence<T> {
    return if (psiClass equivalentTo target) this else filter {
        PsiUtil.getAccessLevel(it.modifierList!!) >= PsiUtil.ACCESS_LEVEL_PROTECTED
    }
}

private fun PsiClass.streamMixinHierarchy(): Sequence<PsiClass> {
    return generateSequence(this) {
        it.superClass?.takeIf { it.isMixin }
    }
}

private class MethodSignature(private val method: PsiMethod) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as MethodSignature

        if (method.name != other.method.name) {
            return false
        }

        return method.isMatchingMethod(other.method)
    }

    override fun hashCode(): Int {
        var result = method.name.hashCode()

        for (parameter in method.parameterList.parameters) {
            result = 31 * result + TypeConversionUtil.erasure(parameter.type).hashCode()
        }

        method.returnType?.let { result = 31 * result + TypeConversionUtil.erasure(it).hashCode() }
        return result
    }
}

private class FieldSignature(private val field: PsiField) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as FieldSignature

        return field.name == other.field.name && field.isMatchingField(other.field)
    }

    override fun hashCode(): Int {
        return 31 * field.name.hashCode() + TypeConversionUtil.erasure(field.type).hashCode()
    }
}

private fun <T> Sequence<T>?.plus(other: Sequence<T>?): Sequence<T>? {
    this ?: return other
    other ?: return this
    return this + other
}
