/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import java.io.Serializable

data class MemberDescriptor(val name: String, val descriptor: String? = null,
                            val owner: String? = null, val matchAll: Boolean = false) : Serializable {

    fun matchOwner(psiClass: PsiClass): Boolean {
        return this.owner == null || this.owner == psiClass.fullQualifiedName
    }

    private fun matchOwner(member: PsiMember, qualifier: PsiClassType?): Boolean {
        return this.owner == null || this.owner == (qualifier?.fullQualifiedName ?: member.containingClass!!.fullQualifiedName)
    }

    fun match(method: PsiMethod, qualifier: PsiClassType? = null): Boolean {
        return this.name == method.internalName && matchOwner(method, qualifier)
                && (this.descriptor == null || this.descriptor == method.descriptor)
    }

    fun match(field: PsiField, qualifier: PsiClassType? = null): Boolean {
        return this.name == field.name && matchOwner(field, qualifier)
                && (this.descriptor == null || this.descriptor == field.descriptor)
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

    companion object {

        /**
         * Parses a [MemberDescriptor] based on the specifications of Mixin's
         * MemberInfo.
         */
        fun parse(descriptor: String): MemberDescriptor? {
            val owner: String?

            var pos = descriptor.lastIndexOf('.')
            if (pos != -1) {
                // Everything before the dot is the qualifier/owner
                owner = descriptor.substring(0, pos)
                if (owner.contains('/')) {
                    // Invalid: Qualifier should only contain dots
                    return null
                }
            } else {
                pos = descriptor.indexOf(';')
                if (pos != -1 && descriptor.startsWith('L')) {
                    val internalOwner = descriptor.substring(1, pos)
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

            val desc: String?
            val name: String
            val matchAll: Boolean

            // Find descriptor separator
            val methodDescPos = descriptor.indexOf('(', pos + 1)
            if (methodDescPos != -1) {
                // Method descriptor
                desc = descriptor.substring(methodDescPos)
                name = descriptor.substring(pos + 1, methodDescPos)
                matchAll = false
            } else {
                val fieldDescPos = descriptor.indexOf(':', pos + 1)
                if (fieldDescPos != -1) {
                    desc = descriptor.substring(fieldDescPos + 1)
                    name = descriptor.substring(pos + 1, fieldDescPos)
                    matchAll = false
                } else {
                    desc = null
                    matchAll = descriptor.endsWith('*')
                    name = if (matchAll) {
                        descriptor.substring(pos + 1, descriptor.lastIndex)
                    } else {
                        descriptor.substring(pos + 1)
                    }
                }
            }

            return MemberDescriptor(name, desc, owner, matchAll)
        }

    }

}

