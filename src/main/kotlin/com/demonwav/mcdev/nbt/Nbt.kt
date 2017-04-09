/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.nbt.tags.NbtTag
import com.demonwav.mcdev.nbt.tags.NbtTypeId
import com.demonwav.mcdev.nbt.tags.RootCompound
import com.demonwav.mcdev.nbt.tags.TagByte
import com.demonwav.mcdev.nbt.tags.TagByteArray
import com.demonwav.mcdev.nbt.tags.TagCompound
import com.demonwav.mcdev.nbt.tags.TagDouble
import com.demonwav.mcdev.nbt.tags.TagEnd
import com.demonwav.mcdev.nbt.tags.TagFloat
import com.demonwav.mcdev.nbt.tags.TagInt
import com.demonwav.mcdev.nbt.tags.TagIntArray
import com.demonwav.mcdev.nbt.tags.TagList
import com.demonwav.mcdev.nbt.tags.TagLong
import com.demonwav.mcdev.nbt.tags.TagShort
import com.demonwav.mcdev.nbt.tags.TagString
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

object Nbt {

    private fun getActualInputStream(stream: InputStream): DataInputStream {
        var tempStream: InputStream? = null
        try {
            tempStream = GZIPInputStream(stream)
        } catch (e: ZipException) {
            tempStream?.close()
            tempStream = stream
            tempStream.reset()
        }
        return DataInputStream(tempStream!!)
    }

    /**
     * Parse the NBT file from the InputStream and return the root TagCompound for the NBT file. This method closes the stream when
     * it is finished with it.
     */
    fun buildTagTree(inputStream: InputStream): RootCompound {
        val stream = getActualInputStream(inputStream)

        stream.use {
            val tagIdByte = stream.readByte()
            val tagId = NbtTypeId.getById(tagIdByte)

            if (tagId != NbtTypeId.COMPOUND) {
                throw MalformedNbtFileException("Root tag in NBT file is not a compound.")
            }

            return RootCompound(stream.readUTF(), stream.readCompoundTag().tagMap)
        }
    }

    private fun DataInputStream.readCompoundTag(): TagCompound {
        val tagMap = HashMap<String, NbtTag>()

        var tagIdByte = this.readByte()
        var tagId = NbtTypeId.getById(tagIdByte)
        while (tagId != NbtTypeId.END) {
            val name = this.readUTF()

            tagMap[name] = this.readTag(tagId)

            tagIdByte = this.readByte()
            tagId = NbtTypeId.getById(tagIdByte)
        }

        return TagCompound(tagMap)
    }

    private fun DataInputStream.readByteTag() = TagByte(this.readByte())
    private fun DataInputStream.readShortTag() = TagShort(this.readShort())
    private fun DataInputStream.readIntTag() = TagInt(this.readInt())
    private fun DataInputStream.readLongTag() = TagLong(this.readLong())
    private fun DataInputStream.readFloatTag() = TagFloat(this.readFloat())
    private fun DataInputStream.readDoubleTag() = TagDouble(this.readDouble())
    private fun DataInputStream.readStringTag() = TagString(this.readUTF())

    private fun DataInputStream.readListTag(): TagList {
        val tagIdByte = this.readByte()
        val tagId = NbtTypeId.getById(tagIdByte)

        val length = this.readInt()
        if (length <= 0) {
            return TagList(tagId, emptyList())
        }

        val list = ArrayList<NbtTag>(length)
        for (i in 0 until length) {
            list.add(this.readTag(tagId))
        }
        return TagList(tagId, list)
    }

    private fun DataInputStream.readByteArrayTag(): TagByteArray {
        val length = this.readInt()

        val bytes = ByteArray(length)
        if (this.read(bytes) != bytes.size) {
            throw RuntimeException()
        }
        return TagByteArray(bytes)
    }

    private fun DataInputStream.readIntArrayTag(): TagIntArray {
        val length = this.readInt()

        val ints = IntArray(length)

        for (i in 0 until length) {
            ints[i] = this.readInt()
        }

        return TagIntArray(ints)
    }

    private fun DataInputStream.readTag(tagId: NbtTypeId): NbtTag {
        when (tagId) {
            NbtTypeId.END -> return TagEnd
            NbtTypeId.BYTE -> return this.readByteTag()
            NbtTypeId.SHORT -> return this.readShortTag()
            NbtTypeId.INT -> return this.readIntTag()
            NbtTypeId.LONG -> return this.readLongTag()
            NbtTypeId.FLOAT -> return this.readFloatTag()
            NbtTypeId.DOUBLE -> return this.readDoubleTag()
            NbtTypeId.BYTE_ARRAY -> return this.readByteArrayTag()
            NbtTypeId.STRING -> return this.readStringTag()
            NbtTypeId.LIST -> return this.readListTag()
            NbtTypeId.COMPOUND -> return this.readCompoundTag()
            NbtTypeId.INT_ARRAY -> return this.readIntArrayTag()
        }
    }
}
