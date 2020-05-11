/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.placeholderapi

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType

class PlaceholderApiModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {
    override val moduleType = PlaceholderApiModuleType
    override val type = PlatformType.PLACEHOLDERAPI
    override val icon = PlatformAssets.PLACEHOLDERAPI_ICON
}
