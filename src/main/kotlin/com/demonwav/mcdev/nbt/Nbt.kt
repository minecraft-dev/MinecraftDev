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
import com.demonwav.mcdev.nbt.tags.bigEndianInt
import com.demonwav.mcdev.nbt.tags.bigEndianLong
import com.demonwav.mcdev.nbt.tags.bigEndianShort
import com.demonwav.mcdev.nbt.tags.littleEndianInt
import com.demonwav.mcdev.nbt.tags.littleEndianLong
import com.demonwav.mcdev.nbt.tags.littleEndianShort
import com.demonwav.mcdev.nbt.tags.toDouble
import com.demonwav.mcdev.nbt.tags.toFloat
import com.demonwav.mcdev.nbt.tags.toUInt
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

class Nbt(private val isBigEndian: Boolean = true) {

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
    fun buildTagTree(inputStream: InputStream): TagCompound {
        val stream = getActualInputStream(inputStream)

        stream.use {
            val tagIdByte = readByte(stream).value
            val tagId = NbtTypeId.getById(tagIdByte)

            if (tagId != NbtTypeId.COMPOUND) {
                throw MalformedNbtFileException("Root tag in NBT file is not a compound.")
            }

            return RootCompound(readString(stream).value, readCompound(stream).tagMap)
        }
    }

    private fun readCompound(stream: DataInputStream): TagCompound {
        val tagMap = HashMap<String, NbtTag>()

        var tagIdByte = readByte(stream).value
        var tagId = NbtTypeId.getById(tagIdByte)
        while (tagId != NbtTypeId.END) {
            val name = readString(stream).value

            tagMap[name] = readTag(stream, tagId)

            tagIdByte = readByte(stream).value
            tagId = NbtTypeId.getById(tagIdByte)
        }

        return TagCompound(tagMap)
    }

    private fun readByte(stream: DataInputStream): TagByte {
        return TagByte(stream.readByte())
    }

    private fun readShort(stream: DataInputStream): TagShort {
        return TagShort(stream.readShort())
    }

    private fun readInt(stream: DataInputStream): TagInt {
        return TagInt(stream.readInt())
    }

    private fun readLong(stream: DataInputStream): TagLong {
        return TagLong(stream.readLong())
    }

    private fun readFloat(stream: DataInputStream): TagFloat {
        return TagFloat(stream.readFloat())
    }

    private fun readDouble(stream: DataInputStream): TagDouble {
        return TagDouble(stream.readDouble())
    }

    private fun readString(stream: DataInputStream): TagString {
        val string = stream.readUTF()
        return TagString(string)
    }

    private fun readList(stream: DataInputStream): TagList {
        val tagIdByte = readByte(stream).value
        val tagId = NbtTypeId.getById(tagIdByte)

        val length = readInt(stream).value
        if (length <= 0) {
            return TagList(tagId, emptyList())
        }

        val list = ArrayList<NbtTag>(length)
        for (i in 0 until length) {
            list.add(readTag(stream, tagId))
        }
        return TagList(tagId, list)
    }

    private fun readByteArray(stream: DataInputStream): TagByteArray {
        val length = readInt(stream).value

        val bytes = ByteArray(length)
        if (stream.read(bytes) != bytes.size) {
            throw RuntimeException()
        }
        return TagByteArray(bytes)
    }

    private fun readIntArray(stream: DataInputStream): TagIntArray {
        val length = readInt(stream).value

        val bytes = ByteArray(length * 4)
        if (stream.read(bytes) != bytes.size) {
            throw RuntimeException()
        }
        val ints = IntArray(length)

        for (i in 0 until length) {
            if (isBigEndian) {
                ints[i] = bytes.bigEndianInt(i * 4)
            } else {
                ints[i] = bytes.littleEndianInt(i * 4)
            }
        }

        return TagIntArray(ints)
    }

    private fun readTag(stream: DataInputStream, tagId: NbtTypeId): NbtTag {
        when (tagId) {
            NbtTypeId.END -> return TagEnd
            NbtTypeId.BYTE -> return readByte(stream)
            NbtTypeId.SHORT -> return readShort(stream)
            NbtTypeId.INT -> return readInt(stream)
            NbtTypeId.LONG -> return readLong(stream)
            NbtTypeId.FLOAT -> return readFloat(stream)
            NbtTypeId.DOUBLE -> return readDouble(stream)
            NbtTypeId.BYTE_ARRAY -> return readByteArray(stream)
            NbtTypeId.STRING -> return readString(stream)
            NbtTypeId.LIST -> return readList(stream)
            NbtTypeId.COMPOUND -> return readCompound(stream)
            NbtTypeId.INT_ARRAY -> return readIntArray(stream)
        }
    }
}
