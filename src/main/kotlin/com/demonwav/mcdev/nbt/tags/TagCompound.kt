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

open class TagCompound(val tagMap: Map<String, NbtTag>) : NbtTag {
    // If a tag doesn't have a name this will throw a NPE
    // but all tags should have names in a compound
    override val payloadSize = tagMap.entries.sumBy { 2 + it.key.toByteArray().size + it.value.payloadSize }
    override val typeId = NbtTypeId.COMPOUND

    override fun write(stream: DataOutputStream) {
        for ((name, tag) in tagMap) {
            // This should absolutely never ever happen
            if (tag.typeId == NbtTypeId.END) {
                continue
            }

            stream.writeByte(tag.typeIdByte.toInt())
            stream.writeUTF(name)
            tag.write(stream)
        }

        stream.write(NbtTypeId.END.typeIdByte.toInt())
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TagCompound) {
            return false
        }

        if (other === this) {
            return true
        }

        if (other.tagMap.size != this.tagMap.size) {
            return false
        }

        return this.tagMap == other.tagMap
    }

    override fun hashCode() = tagMap.hashCode()

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        sb.append('{')

        if (tagMap.isEmpty()) {
            sb.append('}')
            return sb
        }

        sb.append('\n')

        for ((key, value) in tagMap) {
            indent(sb, indentLevel + 1)
            appendName(sb, key)
            value.toString(sb, indentLevel + 1, WriterState.COMPOUND)
            sb.append('\n')
        }

        indent(sb, indentLevel)
        sb.append('}')

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

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        appendName(sb, name)
        super.toString(sb, indentLevel, WriterState.COMPOUND)
        return sb
    }

    override fun copy(): TagCompound {
        val copy = super.copy()
        return RootCompound(name, copy.tagMap)
    }

    override fun write(stream: DataOutputStream) {
        stream.writeByte(NbtTypeId.COMPOUND.typeIdByte.toInt())
        stream.writeUTF(name)
        super.write(stream)
    }
}
