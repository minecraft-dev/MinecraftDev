/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.tags

import java.io.DataOutputStream

class TagIntArray(override val value: IntArray) : NbtValueTag<IntArray>(IntArray::class.java) {
    override val payloadSize = 4 + value.size * 4
    override val typeId = NbtTypeId.INT_ARRAY

    override fun write(stream: DataOutputStream) {
        stream.writeInt(value.size)
        for (i in value) {
            stream.writeInt(i)
        }
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        value.joinTo(buffer = sb, separator = ", ", prefix = "ints(", postfix = ")")
        return sb
    }

    override fun valueEquals(otherValue: IntArray) = this.value.contentEquals(otherValue)

    // This makes IntelliJ happy - and angry at the same time
    @Suppress("RedundantOverride")
    override fun equals(other: Any?) = super.equals(other)

    override fun hashCode() = this.value.contentHashCode()

    override fun valueCopy() = value.copyOf(value.size)
}
