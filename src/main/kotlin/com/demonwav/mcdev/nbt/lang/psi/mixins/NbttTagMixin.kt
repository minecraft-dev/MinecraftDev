/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi.mixins

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
import com.demonwav.mcdev.nbt.lang.psi.NbttElement
import com.demonwav.mcdev.nbt.tags.NbtTypeId

interface NbttTagMixin : NbttElement {

    fun getByte(): NbttByte?
    fun getByteArray(): NbttByteArray?
    fun getCompound(): NbttCompound?
    fun getDouble(): NbttDouble?
    fun getFloat(): NbttFloat?
    fun getInt(): NbttInt?
    fun getIntArray(): NbttIntArray?
    fun getLongArray(): NbttLongArray?
    fun getList(): NbttList?
    fun getLong(): NbttLong?
    fun getShort(): NbttShort?
    fun getString(): NbttString?

    fun getType(): NbtTypeId
    fun getTag(): NbttElement?
}
