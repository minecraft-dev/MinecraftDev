/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.demonwav.mcdev.nbt.tags

import java.io.OutputStream
import kotlin.experimental.and

typealias JFloat = java.lang.Float
typealias JDouble = java.lang.Double

interface NbtTag {

    /**
     * The name of the given [NbtTag], if present, null otherwise.
     */
    val name: String?

    /**
     * The payload size of this tag.
     */
    val payloadSize: Int

    /**
     * The `Type ID` enum value for this tag.
     */
    val typeId: NbtTypeId

    /**
     *  Write out the contents of this tag to the given [OutputStream].
     */
    fun write(stream: OutputStream, isBigEndian: Boolean = true)

    /**
     * toString helper method.
     */
    fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder

    /**
     * Create a deep-copy of this [NbtTag].
     */
    fun copy(): NbtTag
}

// Default implementation via extension properties
/**
 * True if the given [NbtTag] has a name.
 */
val NbtTag.isNamed: Boolean
    get() = name != null

/**
 * The `Type ID` byte value for this tag.
 */
val NbtTag.typeIdByte
    get() = typeId.typeIdByte

/**
 * The `Type ID` tag name for this tag.
 */
val NbtTag.typeName
    get() = typeId.tagName

fun indent(sb: StringBuilder, indentLevel: Int) {
    if (indentLevel <= 0) {
        return
    }

    for (i in 0 until indentLevel) {
        sb.append("    ")
    }
}

fun NbtTag.appendTypeAndName(sb: StringBuilder) {
    sb.append(typeName)

    sb.append("(")
    if (isNamed) {
        sb.append("\"").append(name!!.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")).append("\"")
    } else {
        sb.append("None")
    }
    sb.append("): ")
}

/**
 * If this is a named tag, write it and the type.
 */
fun NbtTag.writeName(stream: OutputStream, isBigEndian: Boolean = true) {
    if (name == null) {
        return
    }

    val nameArray = name!!.toByteArray()

    stream.write(byteArrayOf(
        typeIdByte,
        *(if (isBigEndian) nameArray.size.toShort().toBigEndian() else nameArray.size.toShort().toLittleEndian()),
        *nameArray
    ))
}

// Helper extension methods
fun Short.toBigEndian() = byteArrayOf(
    this.toInt().ushr(8).and(0xFF).toByte(),
    this.and(0xFF).toByte()
)
fun Short.toLittleEndian() = byteArrayOf(
    this.and(0xFF).toByte(),
    this.toInt().ushr(8).and(0xFF).toByte()
)

fun Int.toBigEndian() = byteArrayOf(
    this.ushr(24).and(0xFF).toByte(),
    this.ushr(16).and(0xFF).toByte(),
    this.ushr(8).and(0xFF).toByte(),
    this.and(0xFF).toByte()
)
fun Int.toLittleEndian() = byteArrayOf(
    this.and(0xFF).toByte(),
    this.ushr(8).and(0xFF).toByte(),
    this.ushr(16).and(0xFF).toByte(),
    this.ushr(24).and(0xFF).toByte()
)

fun Long.toBigEndian() = byteArrayOf(
    this.ushr(56).and(0xFF).toByte(),
    this.ushr(48).and(0xFF).toByte(),
    this.ushr(40).and(0xFF).toByte(),
    this.ushr(32).and(0xFF).toByte(),
    this.ushr(24).and(0xFF).toByte(),
    this.ushr(16).and(0xFF).toByte(),
    this.ushr(8).and(0xFF).toByte(),
    this.and(0xFF).toByte()
)
fun Long.toLittleEndian() = byteArrayOf(
    this.and(0xFF).toByte(),
    this.ushr(8).and(0xFF).toByte(),
    this.ushr(16).and(0xFF).toByte(),
    this.ushr(24).and(0xFF).toByte(),
    this.ushr(32).and(0xFF).toByte(),
    this.ushr(40).and(0xFF).toByte(),
    this.ushr(48).and(0xFF).toByte(),
    this.ushr(56).and(0xFF).toByte()
)

fun Float.toByteArray() = JFloat.floatToRawIntBits(this).toBigEndian()
fun Double.toByteArray() = JDouble.doubleToRawLongBits(this).toBigEndian()

fun ByteArray.bigEndianShort(start: Int = 0): Short {
    return (
        this[start].toUInt().shl(8) or
        this[start + 1].toUInt()
    ).toShort()
}
fun ByteArray.littleEndianShort(start: Int = 0): Short {
    return (
        this[start].toUInt() or
        this[start + 1].toUInt().shl(8)
    ).toShort()
}

fun ByteArray.bigEndianInt(start: Int = 0): Int {
    return this[start].toUInt().shl(24) or
        this[start + 1].toUInt().shl(16) or
        this[start + 2].toUInt().shl(8) or
        this[start + 3].toUInt()
}
fun ByteArray.littleEndianInt(start: Int = 0): Int {
    return this[start + 3].toUInt().shl(24) or
        this[start + 2].toUInt().shl(16) or
        this[start + 1].toUInt().shl(8) or
        this[start].toUInt()
}

fun ByteArray.bigEndianLong(start: Int = 0): Long {
    return this[start].toULong().shl(56) or
        this[start + 1].toULong().shl(48) or
        this[start + 2].toULong().shl(40) or
        this[start + 3].toULong().shl(32) or
        this[start + 4].toULong().shl(24) or
        this[start + 5].toULong().shl(16) or
        this[start + 6].toULong().shl(8) or
        this[start + 7].toULong()
}
fun ByteArray.littleEndianLong(start: Int = 0): Long {
    return this[start + 7].toULong().shl(56) or
        this[start + 6].toULong().shl(48) or
        this[start + 5].toULong().shl(40) or
        this[start + 4].toULong().shl(32) or
        this[start + 3].toULong().shl(24) or
        this[start + 2].toULong().shl(16) or
        this[start + 1].toULong().shl(8) or
        this[start].toULong()
}

fun ByteArray.toFloat() = JFloat.intBitsToFloat(this.bigEndianInt())
fun ByteArray.toDouble() = JDouble.longBitsToDouble(this.bigEndianLong())

fun Short.toUInt() = this.toInt().and(0xFF)
fun Byte.toUInt() = this.toInt().and(0xFF)
fun Byte.toULong() = this.toLong().and(0xFF)
