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

class TagFloat(override val value: Float) : NbtValueTag<Float>(Float::class.java) {
    override val payloadSize = 4
    override val typeId = NbtTypeId.FLOAT

    override fun write(stream: DataOutputStream) {
        stream.writeFloat(value)
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        return sb.append(value).append("F")
    }
}
