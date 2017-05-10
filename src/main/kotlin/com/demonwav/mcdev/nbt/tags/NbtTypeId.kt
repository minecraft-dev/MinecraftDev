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

import kotlin.reflect.KClass

enum class NbtTypeId(val typeIdByte: Byte, val tagName: String, val tagClass: KClass<out NbtTag>) {
    END(0.toByte(), "TAG_End", TagEnd::class),
    BYTE(1.toByte(), "TAG_Byte", TagByte::class),
    SHORT(2.toByte(), "TAG_Short", TagShort::class),
    INT(3.toByte(), "TAG_Int", TagInt::class),
    LONG(4.toByte(), "TAG_Long", TagLong::class),
    FLOAT(5.toByte(), "TAG_Float", TagFloat::class),
    DOUBLE(6.toByte(), "TAG_Double", TagDouble::class),
    BYTE_ARRAY(7.toByte(), "TAG_Byte_Array", TagByteArray::class),
    STRING(8.toByte(), "TAG_String", TagString::class),
    LIST(9.toByte(), "TAG_List", TagList::class),
    COMPOUND(10.toByte(), "TAG_Compound", TagCompound::class),
    INT_ARRAY(11.toByte(), "TAG_Int_Array", TagIntArray::class);

    companion object {
        fun getById(id: Byte) = values().firstOrNull { it.typeIdByte == id }
    }
}
