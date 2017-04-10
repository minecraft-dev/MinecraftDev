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

import java.io.DataOutputStream

class TagDouble(override val value: Double) : NbtValueTag<Double>(Double::class.java) {
    override val payloadSize = 8
    override val typeId = NbtTypeId.DOUBLE

    override fun write(stream: DataOutputStream) {
        stream.writeDouble(value)
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()
}
