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
import com.google.common.collect.HashMultiset
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

object Nbt {

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
            val tagIdByte = readByte(stream, isBigEndian, false).value
            val tagId = NbtTypeId.getById(tagIdByte)

            if (tagId != NbtTypeId.COMPOUND) {
                throw MalformedNbtFileException("Root tag in NBT file is not a compound.")
            }

            return readCompound(stream, isBigEndian, true)
        }
    }

    private fun readCompound(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagCompound {
        val name = getName(stream, isBigEndian, isNamed)

        val tagList = HashMultiset.create<NbtTag>()

        var tagIdByte = readByte(stream, isBigEndian, false).value
        var tagId = NbtTypeId.getById(tagIdByte)
        while (tagId != NbtTypeId.END) {
            tagList.add(readTag(stream, isBigEndian, true, tagId))

            tagIdByte = readByte(stream, isBigEndian, false).value
            tagId = NbtTypeId.getById(tagIdByte)
        }

        return TagCompound(name, tagList)
    }

    private fun readByte(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagByte {
        val name = getName(stream, isBigEndian, isNamed)

        return TagByte(name, stream.read().toByte())
    }

    private fun readShort(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagShort {
        val name = getName(stream, isBigEndian, isNamed)

        val bytes = byteArrayOf(stream.read().toByte(), stream.read().toByte())
        if (isBigEndian) {
            return TagShort(name, bytes.bigEndianShort())
        } else {
             return TagShort(name, bytes.littleEndianShort())
        }
    }

    private fun readInt(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagInt {
        val name = getName(stream, isBigEndian, isNamed)

        val bytes = ByteArray(4)
        stream.read(bytes)
        if (isBigEndian) {
            return TagInt(name, bytes.bigEndianInt())
        } else {
            return TagInt(name, bytes.littleEndianInt())
        }
    }

    private fun readLong(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagLong {
        val name = getName(stream, isBigEndian, isNamed)

        val bytes = ByteArray(8)
        stream.read(bytes)
        if (isBigEndian) {
            return TagLong(name, bytes.bigEndianLong())
        } else {
            return TagLong(name, bytes.littleEndianLong())
        }
    }

    private fun readFloat(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagFloat {
        val name = getName(stream, isBigEndian, isNamed)

        val bytes = ByteArray(4)
        stream.read(bytes)
        return TagFloat(name, bytes.toFloat())
    }

    private fun readDouble(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagDouble {
        val name = getName(stream, isBigEndian, isNamed)

        val bytes = ByteArray(8)
        stream.read(bytes)
        return TagDouble(name, bytes.toDouble())
    }

    private fun readString(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagString {
        // We can use the getName() method here, but that would cause this to awkwardly leap-frog back and forth a few times
        val name = if (isNamed) {
            readString(stream, isBigEndian, false).value
        } else {
            null
        }

        val length = readShort(stream, isBigEndian, false).value
        if (length == 0.toShort()) {
            return TagString(name, "")
        }

        val bytes = ByteArray(length.toUInt())
        stream.read(bytes)
        return TagString(name, String(bytes))
    }

    private fun readList(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagList {
        val name = getName(stream, isBigEndian, isNamed)
        
        val tagIdByte = readByte(stream, isBigEndian, false).value
        val tagId = NbtTypeId.getById(tagIdByte)

        val length = readInt(stream, isBigEndian, false).value
        if (length <= 0) {
            return TagList(name, tagId, emptyList())
        }

        val list = ArrayList<NbtTag>(length)
        for (i in 0 until length) {
            list.add(readTag(stream, isBigEndian, false, tagId))
        }
        return TagList(name, tagId, list)
    }

    private fun readByteArray(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagByteArray {
        val name = getName(stream, isBigEndian, isNamed)

        val length = readInt(stream, isBigEndian, false).value

        val bytes = ByteArray(length)
        stream.read(bytes)
        return TagByteArray(name, bytes)
    }

    private fun readIntArray(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): TagIntArray {
        val name = getName(stream, isBigEndian, isNamed)

        val length = readInt(stream, isBigEndian, false).value

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

        return TagIntArray(name, ints)
    }

    private fun getName(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean): String? {
        return if (isNamed) {
            readString(stream, isBigEndian, false).value
        } else {
            null
        }
    }

    private fun readTag(stream: InputStream, isBigEndian: Boolean, isNamed: Boolean, tagId: NbtTypeId): NbtTag {
        when (tagId) {
            NbtTypeId.END -> return TagEnd
            NbtTypeId.BYTE -> return readByte(stream, isBigEndian, isNamed)
            NbtTypeId.SHORT -> return readShort(stream, isBigEndian, isNamed)
            NbtTypeId.INT -> return readInt(stream, isBigEndian, isNamed)
            NbtTypeId.LONG -> return readLong(stream, isBigEndian, isNamed)
            NbtTypeId.FLOAT -> return readFloat(stream, isBigEndian, isNamed)
            NbtTypeId.DOUBLE -> return readDouble(stream, isBigEndian, isNamed)
            NbtTypeId.BYTE_ARRAY -> return readByteArray(stream, isBigEndian, isNamed)
            NbtTypeId.STRING -> return readString(stream, isBigEndian, isNamed)
            NbtTypeId.LIST -> return readList(stream, isBigEndian, isNamed)
            NbtTypeId.COMPOUND -> return readCompound(stream, isBigEndian, isNamed)
            NbtTypeId.INT_ARRAY -> return readIntArray(stream, isBigEndian, isNamed)
        }
    }
}
