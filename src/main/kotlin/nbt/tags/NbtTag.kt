/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        sb.append("\t")
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
