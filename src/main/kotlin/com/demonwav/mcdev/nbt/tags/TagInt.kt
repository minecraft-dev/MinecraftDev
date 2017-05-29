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

class TagInt(override val value: Int) : NbtValueTag<Int>(Int::class.java) {
    override val payloadSize = 4
    override val typeId = NbtTypeId.INT

    override fun write(stream: DataOutputStream) {
        stream.writeInt(value)
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()
}
