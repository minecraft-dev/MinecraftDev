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

class TagFloat(override val name: String?, override val value: Float) : NbtValueTag<Float>(Float::class.java) {
    override val payloadSize = 4
    override val typeId = NbtTypeId.FLOAT

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        writeName(stream, isBigEndian)

        stream.write(value.toByteArray())
    }

    override fun toString() = toString(StringBuilder(), 0).toString()
}
