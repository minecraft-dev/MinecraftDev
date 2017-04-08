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
import java.util.Objects

open class TagCompound(val tagMap: Map<String, NbtTag>) : NbtTag {
    // If a tag doesn't have a name this will throw a NPE
    // but all tags should have names in a compound
    override val payloadSize = tagMap.entries.sumBy { it.key.toByteArray().size + it.value.payloadSize }
    override val typeId = NbtTypeId.COMPOUND

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        for ((name, value) in tagMap) {
            value.writeTypeAndName(stream, name, isBigEndian)
            value.write(stream, isBigEndian)
        }

        stream.write(NbtTypeId.END.typeIdByte.toInt())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TagList) {
            return false
        }

        if (other === this) {
            return true
        }

        if (other.tags.size != this.tagMap.size) {
            return false
        }

        return this.tagMap == other.tags
    }

    override fun hashCode(): Int {
        return Objects.hashCode(tagMap)
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        val entry = if (tagMap.size == 1) {
            "entry"
        } else {
            "entries"
        }
        sb.append(tagMap.size).append(" ").append(entry).append("\n")
        indent(sb, indentLevel)
        sb.append("{\n")

        for ((key, value) in tagMap) {
            indent(sb, indentLevel + 1)
            value.appendTypeAndName(sb, key)
            value.toString(sb, indentLevel + 1)
            sb.append("\n")
        }

        indent(sb, indentLevel)
        sb.append("}")

        return sb
    }

    override fun copy(): TagCompound {
        val newTags = HashMap<String, NbtTag>()
        for ((key, value) in tagMap) {
            newTags[key] = value.copy()
        }
        return TagCompound(newTags)
    }
}

class RootCompound(private val name: String, tagMap: Map<String, NbtTag>) : TagCompound(tagMap) {

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        appendTypeAndName(sb, name)
        super.toString(sb, indentLevel)
        return sb
    }

    override fun copy(): TagCompound {
        val copy = super.copy()
        return RootCompound(name, copy.tagMap)
    }
}
