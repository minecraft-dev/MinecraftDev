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
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.CommonColors
import javax.swing.Icon

object AdventureModuleType : AbstractModuleType<AdventureModule>(
    AdventureConstants.GROUP_ID,
    AdventureConstants.API_ARTIFACT_ID
) {

    const val ID = "ADVENTURE_MODULE_TYPE"

    override val id = ID
    override val platformType = PlatformType.ADVENTURE

    override val icon: Icon = PlatformAssets.ADVENTURE_ICON
    override val isIconSecondary = true

    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()

    init {
        CommonColors.applyStandardColors(colorMap, AdventureConstants.NAMED_TEXT_COLOR_CLASS)
    }

    override fun generateModule(facet: MinecraftFacet) = AdventureModule(facet)
}
