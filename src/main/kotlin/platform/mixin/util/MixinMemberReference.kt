/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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

        builder.append(if (reference.matchAllNames) "*" else reference.name)

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
    fun parse(ref: String?): MemberReference? {
        ref ?: return null
        val reference = ref.replace(" ", "")
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
        val matchAllNames = reference.getOrNull(pos + 1) == '*'
        val matchAllDescs: Boolean

        // Find descriptor separator
        val methodDescPos = reference.indexOf('(', pos + 1)
        if (methodDescPos != -1) {
            // Method descriptor
            descriptor = reference.substring(methodDescPos)
            name = reference.substring(pos + 1, methodDescPos)
            matchAllDescs = false
        } else {
            val fieldDescPos = reference.indexOf(':', pos + 1)
            if (fieldDescPos != -1) {
                descriptor = reference.substring(fieldDescPos + 1)
                name = reference.substring(pos + 1, fieldDescPos)
                matchAllDescs = false
            } else {
                descriptor = null
                matchAllDescs = reference.endsWith('*')
                name = if (matchAllDescs) {
                    reference.substring(pos + 1, reference.lastIndex)
                } else {
                    reference.substring(pos + 1)
                }
            }
        }

        return MemberReference(if (matchAllNames) "*" else name, descriptor, owner, matchAllNames, matchAllDescs)
    }
}
