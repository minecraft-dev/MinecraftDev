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

object TagEnd : NbtTag {
    override val name = null
    override val payloadSize = 0
    override val typeId = NbtTypeId.END

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        stream.write(byteArrayOf(typeIdByte))
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        indent(sb, indentLevel)
        sb.append(NbtTypeId.END.tagName)
        return sb
    }

    override fun copy(): NbtTag {
        return this
    }
}
