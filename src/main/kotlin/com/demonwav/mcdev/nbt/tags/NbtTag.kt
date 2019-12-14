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
import java.io.OutputStream
import org.apache.commons.lang3.StringUtils

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
    fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder

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

val forbiddenCharacters = Regex("""[:(){}\[\],]""")
val badFormat = Regex("""^[\d+\-\\\s\n:{}\[\](),].*|.*["\\:{}\[\]()\s\n,]${'$'}""")

fun writeString(sb: StringBuilder, s: String): StringBuilder {
    if (s.isBlank()) {
        return sb.append('"').append(s.replace("\n", "\\n")).append('"')
    }

    if (s == "bytes" || s == "ints" || s == "longs" || s == "true" || s == "false") {
        // keywords must be quoted
        return sb.append('"').append(s).append('"')
    }

    val replaced = StringUtils.replaceEach(s, arrayOf("\\", "\n", "\"", "\t"), arrayOf("\\\\", "\\n", "\\\"", "\\t"))

    if (forbiddenCharacters in s || s.matches(badFormat)) {
        // Use quotes around this awful string
        return sb.append('"').append(replaced).append('"')
    }

    // prefer no quotes
    return sb.append(replaced)
}

enum class WriterState {
    COMPOUND, LIST
}

fun indent(sb: StringBuilder, indentLevel: Int) {
    if (indentLevel <= 0) {
        return
    }

    for (i in 0 until indentLevel) {
        sb.append("    ")
    }
}

fun appendName(sb: StringBuilder, name: String?) {
    if (name != null) {
        writeString(sb, name)
    } else {
        sb.append("\"\"")
    }
    sb.append(": ")
}
