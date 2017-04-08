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
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

class Nbt {

    /**
     * Rather than creating a byte array over and over again for primitive reads, re-use the same one.
     */
    private val bytes = ByteArray(8)

    private fun getActualInputStream(stream: InputStream): InputStream {
        var tempStream: InputStream? = null
        try {
            tempStream = GZIPInputStream(stream)
        } catch (e: ZipException) {
            tempStream?.close()
            tempStream = stream
            tempStream.reset()
        }
        return tempStream!!
    }

    /**
     * Parse the NBT file from the InputStream and return the root TagCompound for the NBT file. This method closes the stream when
     * it is finished with it.
     */
    fun buildTagTree(inputStream: InputStream, isBigEndian: Boolean): TagCompound {
        val stream = getActualInputStream(inputStream)

        stream.use {
            val tagIdByte = readByte(stream).value
            val tagId = NbtTypeId.getById(tagIdByte)

            if (tagId != NbtTypeId.COMPOUND) {
                throw MalformedNbtFileException("Root tag in NBT file is not a compound.")
            }

            return RootCompound(readString(stream, isBigEndian).value, readCompound(stream, isBigEndian).tagMap)
        }
    }

    private fun readCompound(stream: InputStream, isBigEndian: Boolean): TagCompound {
        val tagMap = HashMap<String, NbtTag>()

        var tagIdByte = readByte(stream).value
        var tagId = NbtTypeId.getById(tagIdByte)
        while (tagId != NbtTypeId.END) {
            val name = readString(stream, isBigEndian).value

            tagMap[name] = readTag(stream, isBigEndian, tagId)

            tagIdByte = readByte(stream).value
            tagId = NbtTypeId.getById(tagIdByte)
        }

        return TagCompound(tagMap)
    }

    private fun readByte(stream: InputStream): TagByte {
        return TagByte(stream.read().toByte())
    }

    private fun readShort(stream: InputStream, isBigEndian: Boolean): TagShort {
        stream.read(bytes, 0, 2)
        if (isBigEndian) {
            return TagShort(bytes.bigEndianShort())
        } else {
             return TagShort(bytes.littleEndianShort())
        }
    }

    private fun readInt(stream: InputStream, isBigEndian: Boolean): TagInt {
        stream.read(bytes, 0, 4)
        if (isBigEndian) {
            return TagInt(bytes.bigEndianInt())
        } else {
            return TagInt(bytes.littleEndianInt())
        }
    }

    private fun readLong(stream: InputStream, isBigEndian: Boolean): TagLong {
        stream.read(bytes, 0, 8)
        if (isBigEndian) {
            return TagLong(bytes.bigEndianLong())
        } else {
            return TagLong(bytes.littleEndianLong())
        }
    }

    private fun readFloat(stream: InputStream): TagFloat {
        stream.read(bytes, 0, 4)
        return TagFloat(bytes.toFloat())
    }

    private fun readDouble(stream: InputStream): TagDouble {
        stream.read(bytes, 0, 8)
        return TagDouble(bytes.toDouble())
    }

    private fun readString(stream: InputStream, isBigEndian: Boolean): TagString {
        val length = readShort(stream, isBigEndian).value
        if (length == 0.toShort()) {
            return TagString.EMPTY_STRING
        }

        val bytes = ByteArray(length.toUInt())
        stream.read(bytes)
        return TagString(String(bytes))
    }

    private fun readList(stream: InputStream, isBigEndian: Boolean): TagList {
        val tagIdByte = readByte(stream).value
        val tagId = NbtTypeId.getById(tagIdByte)

        val length = readInt(stream, isBigEndian).value
        if (length <= 0) {
            return TagList(tagId, emptyList())
        }

        val list = ArrayList<NbtTag>(length)
        for (i in 0 until length) {
            list.add(readTag(stream, isBigEndian, tagId))
        }
        return TagList(tagId, list)
    }

    private fun readByteArray(stream: InputStream, isBigEndian: Boolean): TagByteArray {
        val length = readInt(stream, isBigEndian).value

        val bytes = ByteArray(length)
        stream.read(bytes)
        return TagByteArray(bytes)
    }

    private fun readIntArray(stream: InputStream, isBigEndian: Boolean): TagIntArray {
        val length = readInt(stream, isBigEndian).value

        val bytes = ByteArray(length * 4)
        stream.read(bytes)
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

    private fun readTag(stream: InputStream, isBigEndian: Boolean, tagId: NbtTypeId): NbtTag {
        when (tagId) {
            NbtTypeId.END -> return TagEnd
            NbtTypeId.BYTE -> return readByte(stream)
            NbtTypeId.SHORT -> return readShort(stream, isBigEndian)
            NbtTypeId.INT -> return readInt(stream, isBigEndian)
            NbtTypeId.LONG -> return readLong(stream, isBigEndian)
            NbtTypeId.FLOAT -> return readFloat(stream)
            NbtTypeId.DOUBLE -> return readDouble(stream)
            NbtTypeId.BYTE_ARRAY -> return readByteArray(stream, isBigEndian)
            NbtTypeId.STRING -> return readString(stream, isBigEndian)
            NbtTypeId.LIST -> return readList(stream, isBigEndian)
            NbtTypeId.COMPOUND -> return readCompound(stream, isBigEndian)
            NbtTypeId.INT_ARRAY -> return readIntArray(stream, isBigEndian)
        }
    }
}
