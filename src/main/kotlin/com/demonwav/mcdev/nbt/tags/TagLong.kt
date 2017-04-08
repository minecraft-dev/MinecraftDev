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

class TagLong(override val value: Long) : NbtValueTag<Long>(Long::class.java) {
    override val payloadSize = 8
    override val typeId = NbtTypeId.LONG

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        if (isBigEndian) {
            stream.write(value.toBigEndian())
        } else {
            stream.write(value.toLittleEndian())
        }
    }

    override fun toString() = toString(StringBuilder(), 0).toString()
}
