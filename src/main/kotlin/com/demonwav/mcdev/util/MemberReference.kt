/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.Contract
import java.io.Serializable

/**
 * Represents a reference to a class member (a method or a field). It may
 * resolve to multiple members if [matchAll] is set or if the member is
 * not full qualified.
 */
data class MemberReference(val name: String, val descriptor: String? = null,
                           val owner: String? = null, val matchAll: Boolean = false) : Serializable {

    @get:Contract(pure = true)
    val qualified
        get() = this.owner != null

    @get:Contract(pure = true)
    val withoutOwner
        get() = if (this.owner == null) this else MemberReference(this.name, this.descriptor, null, this.matchAll)

    @Contract(pure = true)
    fun matchOwner(psiClass: PsiClass): Boolean {
        return this.owner == null || this.owner == psiClass.fullQualifiedName
    }

    @Contract(pure = true)
    fun match(method: PsiMethod, qualifier: PsiClass): Boolean {
        return this.name == method.internalName && matchOwner(qualifier)
                && (this.descriptor == null || this.descriptor == method.descriptor)
    }

    @Contract(pure = true)
    fun match(field: PsiField, qualifier: PsiClass): Boolean {
        return this.name == field.name && matchOwner(qualifier)
                && (this.descriptor == null || this.descriptor == field.descriptor)
    }

    fun resolve(project: Project, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)): Pair<PsiClass, PsiMember>? {
        return resolve(project, scope, ::Pair)
    }

    fun resolveMember(project: Project, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)): PsiMember? {
        return resolve(project, scope) { _, member -> member }
    }

    @Contract(pure = true)
    private inline fun <R> resolve(project: Project, scope: GlobalSearchScope, ret: (PsiClass, PsiMember) -> R): R? {
        if (this.owner == null) {
            throw IllegalStateException("Cannot resolve unqualified member reference (owner == null)")
        }

        val psiClass = findQualifiedClass(project, this.owner, scope) ?: return null

        val member: PsiMember? = if (descriptor != null && descriptor.startsWith('(')) {
            // Method, we assume there is only one (since this member descriptor is full qualified)
            psiClass.findMethods(this, checkBases = true).findAny().orElse(null)
        } else {
            // Field
            psiClass.findField(this, checkBases = true)
        }

        return member?.let { ret(psiClass, member) }
    }

}

