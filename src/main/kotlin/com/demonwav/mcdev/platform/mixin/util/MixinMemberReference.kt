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

import com.demonwav.mcdev.util.MemberReference

object MixinMemberReference {

    fun toString(reference: MemberReference): String {
        val builder = StringBuilder()
        if (reference.owner != null) {
            builder.append('L').append(reference.owner.replace('.', '/')).append(';')
        }

        builder.append(reference.name)

        reference.descriptor?.let { descriptor ->
            if (!descriptor.startsWith('(')) {
                // Field descriptor
                builder.append(':')
            }

            builder.append(descriptor)
        }

        return builder.toString()
    }

    /**
     * Parses a [MemberReference] based on the specifications of Mixin's
     * MemberInfo.
     */
    fun parse(reference: String?): MemberReference? {
        reference ?: return null
        val owner: String?

        var pos = reference.lastIndexOf('.')
        if (pos != -1) {
            // Everything before the dot is the qualifier/owner
            owner = reference.substring(0, pos).replace('/', '.')
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
