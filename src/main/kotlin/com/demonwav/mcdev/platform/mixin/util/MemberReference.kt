/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import java.io.Serializable

/**
 * Represents a reference to a class member (a method or a field). It may
 * resolve to multiple members if [matchAll] is set or if the member is
 * not full qualified.
 */
internal data class MemberReference(internal val name: String, internal val descriptor: String? = null,
                                    internal val owner: String? = null, internal val matchAll: Boolean = false) : Serializable {

    internal val qualified
        get() = this.owner != null

    internal val withoutOwner
        get() = if (this.owner == null) this else MemberReference(this.name, this.descriptor, null, this.matchAll)

    internal fun matchOwner(psiClass: PsiClass): Boolean {
        return this.owner == null || this.owner == psiClass.fullQualifiedName
    }

    internal fun match(method: PsiMethod, qualifier: PsiClass): Boolean {
        return this.name == method.internalName && matchOwner(qualifier)
                && (this.descriptor == null || this.descriptor == method.descriptor)
    }

    internal fun match(field: PsiField, qualifier: PsiClass): Boolean {
        return this.name == field.name && matchOwner(qualifier)
                && (this.descriptor == null || this.descriptor == field.descriptor)
    }

    internal fun resolve(project: Project, scope: GlobalSearchScope): Pair<PsiClass, PsiMember>? {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(this.owner!!, scope) ?: return null

        val member: PsiMember? = if (descriptor!!.startsWith('(')) {
            // Method, we assume there is only one (since this member descriptor is full qualified)
            psiClass.findMethods(this, checkBases = true).findAny().orElse(null)
        } else {
            // Field
            psiClass.findField(this, checkBases = true)
        }

        return member?.let { Pair(psiClass, member) }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (owner != null) {
            builder.append('L').append(owner.replace('.', '/')).append(';')
        }

        builder.append(name)

        if (descriptor != null) {
            if (!descriptor.startsWith('(')) {
                // Field descriptor
                builder.append(':')
            }

            builder.append(descriptor)
        }

        return builder.toString()
    }

    internal companion object {

        /**
         * Parses a [MemberReference] based on the specifications of Mixin's
         * MemberInfo. Unlike the parser integrated into Mixin, this method
         * more strict: The specified [String] must strictly match one of
         * the formats, so it should either use the dot separate full qualified
         * class name OR the internal class descriptor.
         */
        internal fun parse(reference: String): MemberReference? {
            val owner: String?

            var pos = reference.lastIndexOf('.')
            if (pos != -1) {
                // Everything before the dot is the qualifier/owner
                owner = reference.substring(0, pos)
                if (owner.contains('/')) {
                    // Invalid: Qualifier should only contain dots
                    return null
                }
            } else {
                pos = reference.indexOf(';')
                if (pos != -1 && reference.startsWith('L')) {
                    val internalOwner = reference.substring(1, pos)
                    if (internalOwner.contains('.')) {
                        // Invalid: Qualifier should only contain slashes
                        return null
                    }

                    owner = internalOwner.replace('/', '.')
                } else {
                    // No owner/qualifier specified
                    pos = -1
                    owner = null
                }
            }

            val descriptor: String?
            val name: String
            val matchAll: Boolean

            // Find descriptor separator
            val methodDescPos = reference.indexOf('(', pos + 1)
            if (methodDescPos != -1) {
                // Method descriptor
                descriptor = reference.substring(methodDescPos)
                name = reference.substring(pos + 1, methodDescPos)
                matchAll = false
            } else {
                val fieldDescPos = reference.indexOf(':', pos + 1)
                if (fieldDescPos != -1) {
                    descriptor = reference.substring(fieldDescPos + 1)
                    name = reference.substring(pos + 1, fieldDescPos)
                    matchAll = false
                } else {
                    descriptor = null
                    matchAll = reference.endsWith('*')
                    name = if (matchAll) {
                        reference.substring(pos + 1, reference.lastIndex)
                    } else {
                        reference.substring(pos + 1)
                    }
                }
            }

            return MemberReference(name, descriptor, owner, matchAll)
        }

    }

}

