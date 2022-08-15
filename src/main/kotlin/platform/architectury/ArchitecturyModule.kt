/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType

class ArchitecturyModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = ArchitecturyModuleType
    override val type = PlatformType.ARCHITECTURY
    override val icon = PlatformAssets.ARCHITECTURY_ICON
}
