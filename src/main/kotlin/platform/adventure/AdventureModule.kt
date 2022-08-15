/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.adventure

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import javax.swing.Icon

class AdventureModule(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = AdventureModuleType
    override val type = PlatformType.ADVENTURE
    override val icon: Icon = PlatformAssets.ADVENTURE_ICON
}
