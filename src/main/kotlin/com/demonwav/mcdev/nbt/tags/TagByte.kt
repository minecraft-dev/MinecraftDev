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

class TagByte(override val value: Byte) : NbtValueTag<Byte>(Byte::class.java) {
    override val payloadSize = 1
    override val typeId = NbtTypeId.BYTE

    override fun write(stream: DataOutputStream) {
        stream.writeByte(value.toInt())
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        if (writerState == WriterState.LIST) {
            return sb.append(value).append("B")
        }

        if (value.toInt() == 1) {
            return sb.append("true")
        } else if (value.toInt() == 0) {
            return sb.append("false")
        }
        return sb.append(value).append("B")
    }
}
