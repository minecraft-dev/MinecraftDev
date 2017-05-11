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
import java.util.Arrays

class TagLongArray(override val value: LongArray) : NbtValueTag<LongArray>(LongArray::class.java) {
    override val payloadSize = 4 + value.size * 8
    override val typeId = NbtTypeId.LONG_ARRAY

    override fun write(stream: DataOutputStream) {
        stream.writeInt(value.size)
        for (l in value) {
            stream.writeLong(l)
        }
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        sb.append("longs(")
        value.joinTo(buffer = sb, separator = ", ")
        sb.append(")")
        return sb
    }

    override fun valueEquals(otherValue: LongArray): Boolean {
        return Arrays.equals(this.value, otherValue)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(this.value)
    }

    override fun valueCopy(): LongArray {
        return Arrays.copyOf(value, value.size)
    }
}
