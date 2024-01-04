/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.nbt.tags

import kotlin.reflect.KClass

enum class NbtTypeId(val typeIdByte: Byte, val tagName: String, val tagClass: KClass<out NbtTag>) {
    END(0, "TAG_End", TagEnd::class),
    BYTE(1, "TAG_Byte", TagByte::class),
    SHORT(2, "TAG_Short", TagShort::class),
    INT(3, "TAG_Int", TagInt::class),
    LONG(4, "TAG_Long", TagLong::class),
    FLOAT(5, "TAG_Float", TagFloat::class),
    DOUBLE(6, "TAG_Double", TagDouble::class),
    BYTE_ARRAY(7, "TAG_Byte_Array", TagByteArray::class),
    STRING(8, "TAG_String", TagString::class),
    LIST(9, "TAG_List", TagList::class),
    COMPOUND(10, "TAG_Compound", TagCompound::class),
    INT_ARRAY(11, "TAG_Int_Array", TagIntArray::class),
    LONG_ARRAY(12, "TAG_Long_Array", TagLongArray::class),
    ;

    companion object {
        fun getById(id: Byte) = values().firstOrNull { it.typeIdByte == id }
    }
}
