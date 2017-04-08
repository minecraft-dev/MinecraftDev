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

class TagByteArray(override val value: ByteArray) : NbtValueTag<ByteArray>(ByteArray::class.java) {
    override val payloadSize = 4 + value.size
    override val typeId = NbtTypeId.BYTE_ARRAY

    override fun write(stream: DataOutputStream) {
        stream.writeInt(value.size)
        stream.write(value)
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        sb.append("[")
        value.joinTo(buffer = sb, separator = ", ")
        sb.append("]")
        return sb
    }

    override fun valueEquals(otherValue: ByteArray): Boolean {
        return Arrays.equals(this.value, otherValue)
    }

    // This makes IntelliJ happy
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(this.value)
    }

    override fun valueCopy(): ByteArray {
        return Arrays.copyOf(value, value.size)
    }
}
