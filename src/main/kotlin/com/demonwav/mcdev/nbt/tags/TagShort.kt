/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.tags

import java.io.DataOutputStream

class TagShort(override val value: Short) : NbtValueTag<Short>(Short::class.java) {
    override val payloadSize = 2
    override val typeId = NbtTypeId.SHORT

    override fun write(stream: DataOutputStream) {
        stream.writeShort(value.toInt())
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        return sb.append(value).append("S")
    }
}
