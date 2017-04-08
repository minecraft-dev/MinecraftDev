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

import java.io.OutputStream
import java.util.Arrays

class TagIntArray(override val value: IntArray) : NbtValueTag<IntArray>(IntArray::class.java) {
    override val payloadSize = 4 + value.size * 4
    override val typeId = NbtTypeId.INT_ARRAY

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        val length = if (isBigEndian) {
            value.size.toBigEndian()
        } else {
            value.size.toLittleEndian()
        }

        val valueArray = ByteArray(value.size * 4)
        for (i in 0 until value.size) {
            val subArray = if (isBigEndian) {
                value[i].toBigEndian()
            } else {
                value[i].toLittleEndian()
            }

            valueArray[i * 4] = subArray[0]
            valueArray[(i * 4) + 1] = subArray[1]
            valueArray[(i * 4) + 2] = subArray[2]
            valueArray[(i * 4) + 3] = subArray[3]
        }

        stream.write(byteArrayOf(*length, *valueArray))
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun valueToString(sb: StringBuilder) {
        sb.append("[")
        value.joinTo(buffer = sb, separator = ", ")
        sb.append("]")
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
