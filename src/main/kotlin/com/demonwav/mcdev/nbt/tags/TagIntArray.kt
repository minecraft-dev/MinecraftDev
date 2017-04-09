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

class TagIntArray(override val value: IntArray) : NbtValueTag<IntArray>(IntArray::class.java) {
    override val payloadSize = 4 + value.size * 4
    override val typeId = NbtTypeId.INT_ARRAY

    override fun write(stream: DataOutputStream) {
        stream.writeInt(value.size)
        for (i in value) {
            stream.writeInt(i)
        }
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        sb.append("ints(\n")
        indent(sb, indentLevel + 1)
        value.joinTo(buffer = sb, separator = ", ")
        sb.append("\n")
        indent(sb, indentLevel)
        sb.append(")")
        return sb
    }

    override fun valueEquals(otherValue: IntArray): Boolean {
        return Arrays.equals(this.value, otherValue)
    }

    // This makes IntelliJ happy.
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(this.value)
    }

    override fun valueCopy(): IntArray {
        return Arrays.copyOf(value, value.size)
    }
}
