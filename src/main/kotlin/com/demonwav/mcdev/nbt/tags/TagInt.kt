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

class TagInt(override val name: String?, override val value: Int) : NbtValueTag<Int>(Int::class.java) {
    override val payloadSize = 4
    override val typeId = NbtTypeId.INT

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        writeName(stream, isBigEndian)

        if (isBigEndian) {
            stream.write(value.toBigEndian())
        } else {
            stream.write(value.toLittleEndian())
        }
    }

    override fun toString() = toString(StringBuilder(), 0).toString()
}
