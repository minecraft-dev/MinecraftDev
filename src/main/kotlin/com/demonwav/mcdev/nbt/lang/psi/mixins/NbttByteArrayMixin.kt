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

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteParams
import com.demonwav.mcdev.nbt.lang.psi.NbttElement
import com.demonwav.mcdev.nbt.tags.TagByteArray

interface NbttByteArrayMixin : NbttElement {

    fun getByteParams(): NbttByteParams?
    fun getByteArrayTag(): TagByteArray
}
