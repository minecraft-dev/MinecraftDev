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

class TagString(override val value: String) : NbtValueTag<String>(String::class.java) {
    override val payloadSize = value.toByteArray().size
    override val typeId = NbtTypeId.STRING

    override fun write(stream: OutputStream, isBigEndian: Boolean) {
        val stringArray = value.toByteArray()

        val length = if (isBigEndian) {
            stringArray.size.toShort().toBigEndian()
        } else {
            stringArray.size.toShort().toLittleEndian()
        }

        stream.write(byteArrayOf(*length, *stringArray))
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    companion object {
        val EMPTY_STRING = TagString("")
    }
}
