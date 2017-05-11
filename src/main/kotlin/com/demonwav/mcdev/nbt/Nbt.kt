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
import com.demonwav.mcdev.nbt.tags.TagLongArray
import com.demonwav.mcdev.nbt.tags.TagShort
import com.demonwav.mcdev.nbt.tags.TagString
import java.io.DataInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

object Nbt {

    private fun getActualInputStream(stream: InputStream): Pair<DataInputStream, Boolean> {
        try {
            return DataInputStream(GZIPInputStream(stream)) to true
        } catch (e: ZipException) {
            stream.reset()
            return DataInputStream(stream) to false
        }
    }

    /**
     * Parse the NBT file from the InputStream and return the root TagCompound for the NBT file. This method closes the stream when
     * it is finished with it.
     */
    fun buildTagTree(inputStream: InputStream, timeout: Long): Pair<RootCompound, Boolean> {
        val (stream, isCompressed) = getActualInputStream(inputStream)

        stream.use {
            val tagIdByte = stream.readByte()
            val tagId = NbtTypeId.getById(tagIdByte) ?: throw MalformedNbtFileException("Unexpected tag id found: $tagIdByte")

            if (tagId != NbtTypeId.COMPOUND) {
                throw MalformedNbtFileException("Root tag in NBT file is not a compound.")
            }

            val start = System.currentTimeMillis()

            return RootCompound(stream.readUTF(), stream.readCompoundTag(start, timeout).tagMap) to isCompressed
        }
    }

    private fun DataInputStream.readCompoundTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val tagMap = HashMap<String, NbtTag>()

        var tagIdByte = this.readByte()
        var tagId = NbtTypeId.getById(tagIdByte) ?: throw MalformedNbtFileException("Unexpected tag id found: $tagIdByte")
        while (tagId != NbtTypeId.END) {
            val name = this.readUTF()

            tagMap[name] = this.readTag(tagId, start, timeout)

            tagIdByte = this.readByte()
            tagId = NbtTypeId.getById(tagIdByte) ?: throw MalformedNbtFileException("Unexpected tag id found: $tagIdByte")
        }

        return@checkTimeout TagCompound(tagMap)
    }

    private fun DataInputStream.readByteTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagByte(this.readByte()) }
    private fun DataInputStream.readShortTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagShort(this.readShort()) }
    private fun DataInputStream.readIntTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagInt(this.readInt()) }
    private fun DataInputStream.readLongTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagLong(this.readLong()) }
    private fun DataInputStream.readFloatTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagFloat(this.readFloat()) }
    private fun DataInputStream.readDoubleTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagDouble(this.readDouble()) }
    private fun DataInputStream.readStringTag(start: Long, timeout: Long) = checkTimeout(start, timeout) { TagString(this.readUTF()) }

    private fun DataInputStream.readListTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val tagIdByte = this.readByte()
        val tagId = NbtTypeId.getById(tagIdByte) ?: throw MalformedNbtFileException("Unexpected tag id found: $tagIdByte")

        val length = this.readInt()
        if (length <= 0) {
            return@checkTimeout TagList(tagId, emptyList())
        }

        val list = ArrayList<NbtTag>(length)
        for (i in 0 until length) {
            list.add(this.readTag(tagId, start, timeout))
        }
        return@checkTimeout TagList(tagId, list)
    }

    private fun DataInputStream.readByteArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val bytes = ByteArray(length)
        if (this.read(bytes) != bytes.size) {
            throw RuntimeException()
        }
        return@checkTimeout TagByteArray(bytes)
    }

    private fun DataInputStream.readIntArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val ints = IntArray(length)

        for (i in 0 until length) {
            ints[i] = this.readInt()
        }

        return@checkTimeout TagIntArray(ints)
    }

    private fun DataInputStream.readLongArrayTag(start: Long, timeout: Long) = checkTimeout(start, timeout) {
        val length = this.readInt()

        val longs = LongArray(length)
        for (i in 0 until length) {
            longs[i] = this.readLong()
        }

        return@checkTimeout TagLongArray(longs)
    }

    private fun DataInputStream.readTag(tagId: NbtTypeId, start: Long, timeout: Long): NbtTag {
        when (tagId) {
            NbtTypeId.END -> return TagEnd
            NbtTypeId.BYTE -> return this.readByteTag(start, timeout)
            NbtTypeId.SHORT -> return this.readShortTag(start, timeout)
            NbtTypeId.INT -> return this.readIntTag(start, timeout)
            NbtTypeId.LONG -> return this.readLongTag(start, timeout)
            NbtTypeId.FLOAT -> return this.readFloatTag(start, timeout)
            NbtTypeId.DOUBLE -> return this.readDoubleTag(start, timeout)
            NbtTypeId.BYTE_ARRAY -> return this.readByteArrayTag(start, timeout)
            NbtTypeId.STRING -> return this.readStringTag(start, timeout)
            NbtTypeId.LIST -> return this.readListTag(start, timeout)
            NbtTypeId.COMPOUND -> return this.readCompoundTag(start, timeout)
            NbtTypeId.INT_ARRAY -> return this.readIntArrayTag(start, timeout)
            NbtTypeId.LONG_ARRAY -> return this.readLongArrayTag(start, timeout)
        }
    }

    private fun <T : Any> checkTimeout(start: Long, timeout: Long, action: () -> T): T {
        val now = System.currentTimeMillis()
        val took = now - start

        if (took > timeout) {
            throw MalformedNbtFileException("NBT parse timeout exceeded - Parse time: $took, Timeout: $timeout.")
        }

        return action()
    }
}
