/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DYNAMIC
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.resolveClass
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.search.GlobalSearchScope
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

fun PsiMember.findUpstreamMixin(): PsiClass? {
    return findAnnotation(DYNAMIC)?.findDeclaredAttributeValue("mixin")?.resolveClass()
}

data class ClassAndMethodNode(val clazz: ClassNode, val method: MethodNode)

fun findMethods(psiClass: PsiClass, allowClinit: Boolean = true): Sequence<ClassAndMethodNode>? {
    val targets = psiClass.mixinTargets
    return when (targets.size) {
        0 -> null
        1 ->
            targets.single().let { target ->
                target.methods?.asSequence()
                    ?.filter { !it.isConstructor && (allowClinit || !it.isClinit) }
                    ?.map { ClassAndMethodNode(target, it) }
            }
        else ->
            targets.asSequence()
                .flatMap { target ->
                    target.methods?.asSequence()?.map { ClassAndMethodNode(target, it) } ?: emptySequence()
                }
                .filter { !it.method.isConstructor && (allowClinit || !it.method.isClinit) }
                .groupBy { it.method.memberReference }
                .values.asSequence()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter { classAndMethod ->
        // Filter methods which are already in the Mixin class
        !psiClass.findMethods(classAndMethod.method.memberReference, false).any()
    }
}

data class ClassAndFieldNode(val clazz: ClassNode, val field: FieldNode)

fun findFields(psiClass: PsiClass): Sequence<ClassAndFieldNode>? {
    val targets = psiClass.mixinTargets
    return when (targets.size) {
        0 -> null
        1 ->
            targets.single().let { target ->
                target.fields?.asSequence()?.map { ClassAndFieldNode(target, it) }
            }
        else ->
            targets.asSequence()
                .flatMap { target ->
                    target.fields?.asSequence()?.map { ClassAndFieldNode(target, it) } ?: emptySequence()
                }
                .groupBy { it.field.memberReference }
                .values.asSequence()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter {
        // Filter fields which are already in the Mixin class
        psiClass.findFieldByName(it.field.name, false) == null
    }
}

fun findShadowTargets(psiClass: PsiClass, start: PsiClass, superMixin: Boolean): Sequence<MixinTargetMember> {
    return if (superMixin) {
        findShadowTargetsDeep(psiClass, start)
    } else {
        // No need to walk the hierarchy if we don't have a super mixin
        findMethods(start)?.map { MethodTargetMember(it) }
            .plus(findFields(start)?.map { FieldTargetMember(it) }) ?: emptySequence()
    }
}

private fun findShadowTargetsDeep(psiClass: PsiClass, start: PsiClass): Sequence<MixinTargetMember> {
    return start.streamMixinHierarchy()
        .flatMap { mixin ->
            val actualMixin = mixin.takeIf { !(it equivalentTo psiClass) }
            findMethods(mixin)?.map { MethodTargetMember(it, actualMixin) }
                .plus(findFields(mixin)?.map { FieldTargetMember(it, actualMixin) })
                ?.filterAccessible(psiClass, mixin) ?: emptySequence()
        }
        .distinctBy {
            when (it) {
                is MethodTargetMember -> it.classAndMethod.method.memberReference
                is FieldTargetMember -> it.classAndField.field.memberReference
            }
        }
}

sealed class MixinTargetMember(val mixin: PsiClass?) {
    abstract val access: Int

    abstract fun findSourceElement(
        project: Project,
        scope: GlobalSearchScope,
        canDecompile: Boolean = false
    ): PsiElement?

    abstract fun findOrConstructSourceMember(
        project: Project,
        scope: GlobalSearchScope,
        canDecompile: Boolean = false
    ): PsiMember
}

class FieldTargetMember(val classAndField: ClassAndFieldNode, mixin: PsiClass? = null) : MixinTargetMember(mixin) {
    constructor(clazz: ClassNode, field: FieldNode) : this(ClassAndFieldNode(clazz, field))

    override val access = classAndField.field.access

    override fun findSourceElement(project: Project, scope: GlobalSearchScope, canDecompile: Boolean) =
        classAndField.field.findSourceField(classAndField.clazz, project, scope, canDecompile)

    override fun findOrConstructSourceMember(
        project: Project,
        scope: GlobalSearchScope,
        canDecompile: Boolean
    ) = classAndField.field.findOrConstructSourceField(classAndField.clazz, project, scope, canDecompile)
}

class MethodTargetMember(val classAndMethod: ClassAndMethodNode, mixin: PsiClass? = null) : MixinTargetMember(mixin) {
    constructor(clazz: ClassNode, method: MethodNode) : this(ClassAndMethodNode(clazz, method))

    override val access = classAndMethod.method.access

    override fun findSourceElement(project: Project, scope: GlobalSearchScope, canDecompile: Boolean) =
        classAndMethod.method.findSourceElement(classAndMethod.clazz, project, scope, canDecompile)

    override fun findOrConstructSourceMember(
        project: Project,
        scope: GlobalSearchScope,
        canDecompile: Boolean
    ) = classAndMethod.method.findOrConstructSourceMethod(classAndMethod.clazz, project, scope, canDecompile)
}

private fun Sequence<MixinTargetMember>.filterAccessible(
    psiClass: PsiClass,
    target: PsiClass,
): Sequence<MixinTargetMember> {
    return if (psiClass equivalentTo target) {
        this
    } else {
        filter {
            (it.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED)) != 0
        }
    }
}

private fun PsiClass.streamMixinHierarchy(): Sequence<PsiClass> {
    return generateSequence(this) {
        it.superClass?.takeIf { superClass -> superClass.isMixin }
    }
}

private fun <T> Sequence<T>?.plus(other: Sequence<T>?): Sequence<T>? {
    this ?: return other
    other ?: return this
    return this + other
}
