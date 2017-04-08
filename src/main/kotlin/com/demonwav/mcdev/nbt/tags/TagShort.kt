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

class TagShort(override val value: Short) : NbtValueTag<Short>(Short::class.java) {
    override val payloadSize = 2
    override val typeId = NbtTypeId.SHORT

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        if (isBigEndian) {
            stream.write(value.toBigEndian())
        } else {
            stream.write(value.toLittleEndian())
        }
    }

    override fun toString() = toString(StringBuilder(), 0).toString()
}
