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

import com.google.common.collect.HashMultiset
import com.google.common.collect.Multiset
import java.io.OutputStream
import java.util.Objects

class TagCompound(override val name: String?, val tags: Multiset<NbtTag>) : NbtTag {
    // If a tag doesn't have a name this will throw a NPE
    // but all tags should have names in a compound
    override val payloadSize = tags.sumBy { it.name!!.toByteArray().size + it.payloadSize }
    override val typeId = NbtTypeId.COMPOUND

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        writeName(stream, isBigEndian)

        tags.forEach { it.write(stream, isBigEndian) }

        stream.write(byteArrayOf(NbtTypeId.END.typeIdByte))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TagList) {
            return false
        }

        if (other === this) {
            return true
        }

        if (other.name != this.name) {
            return false
        }

        if (other.tags.size != this.tags.size) {
            return false
        }

        return this.tags == other.tags
    }

    override fun hashCode(): Int {
        return Objects.hash(name, tags)
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        indent(sb, indentLevel)

        appendTypeAndName(sb)

        val entry = if (tags.size == 1) {
            "entry"
        } else {
            "entries"
        }
        sb.append(tags.size).append(" ").append(entry).append("\n")
        indent(sb, indentLevel)
        sb.append("{\n")

        for (tag in tags) {
            tag.toString(sb, indentLevel + 1)
            sb.append("\n")
        }

        indent(sb, indentLevel)
        sb.append("}")

        return sb
    }

    override fun copy(): TagCompound {
        val newTags = HashMultiset.create<NbtTag>()
        tags.mapTo(newTags) { it.copy() }
        return TagCompound(name, newTags)
    }
}
