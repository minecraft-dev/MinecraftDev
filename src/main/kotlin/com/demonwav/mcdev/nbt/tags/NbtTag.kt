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

import java.io.DataOutputStream
import java.io.OutputStream

typealias JFloat = java.lang.Float
typealias JDouble = java.lang.Double

interface NbtTag {

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
    fun write(stream: DataOutputStream)

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

fun NbtTag.appendTypeAndName(sb: StringBuilder, name: String?) {
    sb.append(typeName)

    sb.append("(")
    if (name != null) {
        sb.append("\"").append(name.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")).append("\"")
    } else {
        sb.append("None")
    }
    sb.append("): ")
}
