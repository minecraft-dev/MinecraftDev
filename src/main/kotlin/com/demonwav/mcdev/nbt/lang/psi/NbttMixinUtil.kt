/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi

import com.demonwav.mcdev.nbt.MalformedNbtFileException
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByte
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttCompound
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttDouble
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttFloat
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttInt
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttList
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLong
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLongArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttShort
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttString
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTag
import com.demonwav.mcdev.nbt.tags.NbtTag
import com.demonwav.mcdev.nbt.tags.NbtTypeId

fun NbttTag.getNtbTag(): NbtTag {
    return when (this.getType()) {
        NbtTypeId.BYTE -> (getTag() as NbttByte).getByteTag()
        NbtTypeId.SHORT -> (getTag() as NbttShort).getShortTag()
        NbtTypeId.INT -> (getTag() as NbttInt).getIntTag()
        NbtTypeId.LONG -> (getTag() as NbttLong).getLongTag()
        NbtTypeId.FLOAT -> (getTag() as NbttFloat).getFloatTag()
        NbtTypeId.DOUBLE -> (getTag() as NbttDouble).getDoubleTag()
        NbtTypeId.BYTE_ARRAY -> (getTag() as NbttByteArray).getByteArrayTag()
        NbtTypeId.STRING -> (getTag() as NbttString).getStringTag()
        NbtTypeId.LIST -> (getTag() as NbttList).getListTag()
        NbtTypeId.COMPOUND -> (getTag() as NbttCompound).getCompoundTag()
        NbtTypeId.INT_ARRAY -> (getTag() as NbttIntArray).getIntArrayTag()
        NbtTypeId.LONG_ARRAY -> (getTag() as NbttLongArray).getLongArrayTag()
        else -> throw MalformedNbtFileException("Unknown error")
    }
}
