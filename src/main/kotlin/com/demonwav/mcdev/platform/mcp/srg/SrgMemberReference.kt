/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.util.MemberReference

object SrgMemberReference {

    fun toString(reference: MemberReference): String {
        return reference.owner!!.replace('.', '/') + '/' + reference.name + (reference.descriptor ?: "")
    }

    fun parse(reference: String, descriptor: String? = null): MemberReference {
        val pos = reference.lastIndexOf('/')
        return MemberReference(reference.substring(pos + 1), descriptor, reference.substring(0, pos).replace('/', '.'))
    }
}
