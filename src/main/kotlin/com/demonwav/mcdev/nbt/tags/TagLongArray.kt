/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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
        value.joinTo(buffer = sb, separator = ", ", prefix = "longs(", postfix = ")")
        return sb
    }

    override fun valueEquals(otherValue: LongArray) = Arrays.equals(this.value, otherValue)

    // This makes IntelliJ happy - and angry at the same time
    @Suppress("RedundantOverride")
    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = Arrays.hashCode(this.value)

    override fun valueCopy() = Arrays.copyOf(value, value.size)!!
}
