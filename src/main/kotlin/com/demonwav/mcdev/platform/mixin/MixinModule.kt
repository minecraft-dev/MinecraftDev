/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import javax.swing.Icon

class MixinModule(facet: MinecraftFacet) : AbstractModule(facet) {

    override val moduleType = MixinModuleType
    override val type = PlatformType.MIXIN
    override val icon: Icon? = null
}
