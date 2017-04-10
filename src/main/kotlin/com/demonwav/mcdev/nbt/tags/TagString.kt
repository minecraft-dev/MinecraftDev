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

class TagString(override val value: String) : NbtValueTag<String>(String::class.java) {
    override val payloadSize = 2 + value.toByteArray().size
    override val typeId = NbtTypeId.STRING

    override fun write(stream: DataOutputStream) {
        stream.writeUTF(value)
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        return writeString(sb, value)
    }
}
