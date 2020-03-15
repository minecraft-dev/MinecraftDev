/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi.mixins

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLong
import com.demonwav.mcdev.nbt.lang.psi.NbttElement
import com.demonwav.mcdev.nbt.tags.TagLongArray

interface NbttLongArrayMixin : NbttElement {

    fun getLongList(): List<NbttLong>
    fun getLongArrayTag(): TagLongArray
}
