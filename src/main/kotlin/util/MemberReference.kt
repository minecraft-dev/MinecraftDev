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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.bytecode
import com.demonwav.mcdev.platform.mixin.util.findField
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.objectweb.asm.Type

/**
 * Represents a reference to a class member (a method or a field).
 */
data class MemberReference(
    val name: String,
    val descriptor: String? = null,
    override val owner: String? = null,
) : MemberMatcher {

    init {
        assert(owner?.contains('/') != true)
    }

    val withoutDescriptor
        get() = if (this.descriptor == null) {
            this
        } else {
            copy(descriptor = null)
        }

    val withoutOwner
        get() = if (this.owner == null) {
            this
        } else {
            copy(owner = null)
        }

    override val methodDescriptor = descriptor?.takeIf { it.contains("(") }
    override val fieldDescriptor = descriptor?.takeUnless { it.contains("(") }

    val presentableText: String get() = buildString {
        if (owner != null) {
            append(owner.substringAfterLast('.'))
            append('.')
        }
        append(name)
        if (descriptor != null && descriptor.startsWith("(")) {
            append('(')
            append(Type.getArgumentTypes(descriptor).joinToString { it.className.substringAfterLast('.') })
            append(')')
        }
    }

    override fun canEverMatch(name: String): Boolean {
        return this.name == name
    }

    private fun matchOwner(clazz: String): Boolean {
        assert(!clazz.contains('.'))
        return this.owner == null || this.owner == clazz.replace('/', '.')
    }

    override fun matchField(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return this.name == name &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
    }

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        assert(!owner.contains('.'))
        return this.name == name &&
            matchOwner(owner) &&
            (this.descriptor == null || this.descriptor == desc)
    }

    fun toMixinString() = buildString {
        if (owner != null) {
            append('L').append(owner.replace('.', '/')).append(';')
        }

        append(name)

        descriptor?.let { descriptor ->
            if (!descriptor.startsWith('(')) {
                // Field descriptor
                append(':')
            }

            append(descriptor)
        }
    }

    fun resolveMember(project: Project, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)): PsiMember? {
        return resolve(project, scope) { _, member -> member }
    }

    fun resolveAsm(
        project: Project,
        scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
    ): MixinTargetMember? {
        val owner = this.owner ?: return null

        fun doFind(owner: String): MixinTargetMember? {
            if (owner == CommonClassNames.JAVA_LANG_OBJECT) {
                return null
            }
            return RecursionManager.doPreventingRecursion(owner, false) {
                val classNode = findQualifiedClass(project, owner, scope)?.bytecode ?: return@doPreventingRecursion null

                classNode.findMethod(this)?.let {
                    return@doPreventingRecursion MethodTargetMember(classNode, it)
                }

                classNode.findField(this)?.let {
                    return@doPreventingRecursion FieldTargetMember(classNode, it)
                }

                classNode.superName?.let { doFind(it.replace('/', '.')) }?.let { return@doPreventingRecursion it }

                classNode.interfaces?.let { interfaces ->
                    for (itf in interfaces) {
                        doFind(itf.replace('/', '.'))?.let { return@doPreventingRecursion it }
                    }
                }

                null
            }
        }

        return doFind(owner)
    }

    private inline fun <R> resolve(project: Project, scope: GlobalSearchScope, ret: (PsiClass, PsiMember) -> R): R? {
        val owner = this.owner ?: return null

        val psiClass = findQualifiedClass(project, owner, scope) ?: return null

        val field = psiClass.findField(this, checkBases = true)
        return if (field != null) {
            ret(psiClass, field)
        } else {
            psiClass.findMethods(this, checkBases = true).firstOrNull()?.let { ret(psiClass, it) }
        }
    }
}

// Class

fun PsiClass.findMethods(member: MemberReference, checkBases: Boolean = false): Sequence<PsiMethod> {
    val methods = if (checkBases) {
        allMethods.asSequence()
    } else {
        methods.asSequence()
    } + constructors
    return methods.filter { member.matchMethod(it, this) }
}

fun PsiClass.findField(selector: MemberReference, checkBases: Boolean = false): PsiField? {
    val fields = if (checkBases) {
        allFields.toList()
    } else {
        fields.toList()
    }
    return fields.firstOrNull { selector.matchField(it, this) }
}

// Method

val PsiMethod.memberReference
    get() = MemberReference(internalName, descriptor)

val PsiMethod.qualifiedMemberReference
    get() = MemberReference(internalName, descriptor, containingClass?.fullQualifiedName)

fun PsiMethod.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return getQualifiedMemberReference(owner.fullQualifiedName)
}

fun PsiMethod.getQualifiedMemberReference(owner: String?): MemberReference {
    return MemberReference(internalName, descriptor, owner)
}

fun PsiMethod?.isSameReference(reference: PsiMethod?): Boolean =
    this != null && (this === reference || qualifiedMemberReference == reference?.qualifiedMemberReference)

// Field
val PsiField.simpleMemberReference
    get() = MemberReference(name)

val PsiField.memberReference
    get() = MemberReference(name, descriptor)

val PsiField.simpleQualifiedMemberReference
    get() = MemberReference(name, null, containingClass!!.fullQualifiedName)

val PsiField.qualifiedMemberReference
    get() = MemberReference(name, descriptor, containingClass!!.fullQualifiedName)

fun PsiField.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return getQualifiedMemberReference(owner.fullQualifiedName)
}

fun PsiField.getQualifiedMemberReference(owner: String?): MemberReference {
    return MemberReference(name, descriptor, owner)
}
