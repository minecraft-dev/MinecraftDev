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

import com.demonwav.mcdev.nbt.lang.NbttFile
import com.demonwav.mcdev.nbt.lang.NbttFileType
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttRootCompound
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import java.io.DataOutputStream
import java.util.Objects

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

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        sb.append("{")

        if (tagMap.isEmpty()) {
            sb.append("}")
            return sb
        }

        sb.append("\n")

        for ((key, value) in tagMap) {
            indent(sb, indentLevel + 1)
            appendName(sb, key)
            value.toString(sb, indentLevel + 1, WriterState.COMPOUND)
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

    fun buildPsi(project: Project): NbttRootCompound {
        val sb = StringBuilder()
        toString(sb, 0, WriterState.COMPOUND)
        return (PsiFileFactory.getInstance(project).createFileFromText("name", NbttFileType, sb.toString()) as NbttFile)
            .firstChild as NbttRootCompound
    }
}
