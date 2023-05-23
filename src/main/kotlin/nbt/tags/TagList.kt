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
import java.util.Objects

class TagList(val type: NbtTypeId, val tags: List<NbtTag>) : NbtTag {
    // TAG_List has nameless tags, so we don't need to do anything for the names of tags
    override val payloadSize = 5 + tags.sumOf { it.payloadSize }
    override val typeId = NbtTypeId.LIST

    override fun write(stream: DataOutputStream) {
        stream.writeByte(type.typeIdByte.toInt())
        stream.writeInt(tags.size)
        tags.forEach { it.write(stream) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TagList) {
            return false
        }

        if (other === this) {
            return true
        }

        if (other.type != this.type) {
            return false
        }

        if (other.tags.size != this.tags.size) {
            return false
        }

        return (0 until this.tags.size).all { i -> other.tags[i] == this.tags[i] }
    }

    override fun hashCode() = Objects.hash(type, tags)

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        sb.append('[')

        if (tags.isEmpty()) {
            sb.append(']')
            return sb
        }

        val isCollection = when (type) {
            NbtTypeId.COMPOUND, NbtTypeId.LIST, NbtTypeId.BYTE_ARRAY, NbtTypeId.INT_ARRAY, NbtTypeId.LONG_ARRAY -> true
            else -> false
        }
        for (tag in tags) {
            if (isCollection) {
                sb.append('\n')
                indent(sb, indentLevel + 1)
            } else {
                sb.append(' ')
            }

            tag.toString(sb, indentLevel + 1, WriterState.LIST)
            sb.append(',')
        }

        if (isCollection) {
            sb.append('\n')
            indent(sb, indentLevel)
            sb.append(']')
        } else {
            sb.append(" ]")
        }

        return sb
    }

    override fun copy(): TagList {
        val newTags = ArrayList<NbtTag>(tags.size)
        tags.mapTo(newTags) { it.copy() }
        return TagList(type, newTags)
    }
}
