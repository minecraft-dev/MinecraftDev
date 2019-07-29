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

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttInt
import com.demonwav.mcdev.nbt.lang.psi.NbttElement
import com.demonwav.mcdev.nbt.tags.TagIntArray

interface NbttIntArrayMixin : NbttElement {

    fun getIntList(): List<NbttInt>
    fun getIntArrayTag(): TagIntArray
}
