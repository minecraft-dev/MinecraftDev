/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt

import com.demonwav.mcdev.framework.findLibraryPath
import com.demonwav.mcdev.nbt.tags.NbtTypeId
import com.demonwav.mcdev.nbt.tags.RootCompound
import com.demonwav.mcdev.nbt.tags.TagByte
import com.demonwav.mcdev.nbt.tags.TagByteArray
import com.demonwav.mcdev.nbt.tags.TagCompound
import com.demonwav.mcdev.nbt.tags.TagDouble
import com.demonwav.mcdev.nbt.tags.TagFloat
import com.demonwav.mcdev.nbt.tags.TagInt
import com.demonwav.mcdev.nbt.tags.TagIntArray
import com.demonwav.mcdev.nbt.tags.TagList
import com.demonwav.mcdev.nbt.tags.TagLong
import com.demonwav.mcdev.nbt.tags.TagLongArray
import com.demonwav.mcdev.nbt.tags.TagShort
import com.demonwav.mcdev.nbt.tags.TagString
import com.intellij.util.io.inputStream
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NBT Parse Tests")
class NbtParseTest {

    private lateinit var nbtFile: Path

    @BeforeEach
    fun setup() {
        nbtFile = Paths.get(findLibraryPath("all-types-nbt"))
    }

    @Test
    @DisplayName("NBT Parse Test")
    fun parseTest() {
        val (compound, compressed) = Nbt.buildTagTree(nbtFile.inputStream(), 1000L)
        Assertions.assertTrue(compressed)
        Assertions.assertEquals(expected, compound)
    }

    @Test
    @DisplayName("NBT Parse Timeout Test")
    fun slowParseTest() {
        Assertions.assertThrows(MalformedNbtFileException::class.java) {
            Nbt.buildTagTree(nbtFile.inputStream(), -1L)
        }
    }

    private val expected: RootCompound =
        RootCompound("root", mapOf(
            "byte" to TagByte(1),
            "short" to TagShort(127),
            "int" to TagInt(127),
            "long" to TagLong(127),
            "float" to TagFloat(127F),
            "double" to TagDouble(127.0),
            "byteArray" to TagByteArray(byteArrayOf(1)),
            "intArray" to TagIntArray(intArrayOf(127)),
            "longArray" to TagLongArray(longArrayOf(127)),
            "byteList" to TagList(NbtTypeId.BYTE, listOf(TagByte(1))),
            "shortList" to TagList(NbtTypeId.SHORT, listOf(TagShort(127))),
            "intList" to TagList(NbtTypeId.INT, listOf(TagInt(127))),
            "longList" to TagList(NbtTypeId.LONG, listOf(TagLong(127))),
            "floatList" to TagList(NbtTypeId.FLOAT, listOf(TagFloat(127F))),
            "doubleList" to TagList(NbtTypeId.DOUBLE, listOf(TagDouble(127.0))),
            "string" to TagString("this is a string"),
            "compound1" to TagCompound(mapOf(
                "compound2" to TagCompound(mapOf(
                    "compound3" to TagCompound(mapOf(
                        "list" to TagList(NbtTypeId.COMPOUND, listOf(
                            TagCompound(mapOf(
                                "key" to TagString("value")
                            )),
                            TagCompound(mapOf(
                                "key" to TagString("value")
                            ))
                        ))
                    ))
                ))
            ))
        ))
}
