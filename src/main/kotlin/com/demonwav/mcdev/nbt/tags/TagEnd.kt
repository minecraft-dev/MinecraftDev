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

object TagEnd : NbtTag {
    override val payloadSize = 0
    override val typeId = NbtTypeId.END

    override fun write(stream: DataOutputStream) {}

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        return sb
    }

    override fun copy(): NbtTag {
        return this
    }
}
