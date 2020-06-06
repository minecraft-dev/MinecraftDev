/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.CommonColors

object VelocityModuleType : AbstractModuleType<VelocityModule>("com.velocitypowered", "velocity") {

    private const val ID = "VELOCITY_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, VelocityConstants.KYORI_TEXT_COLOR)
    }

    override val platformType = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON
    override val id = ID

    override val ignoredAnnotations =
        listOf(VelocityConstants.SUBSCRIBE_ANNOTATION, VelocityConstants.PLUGIN_ANNOTATION)
    override val listenerAnnotations =
        listOf(VelocityConstants.SUBSCRIBE_ANNOTATION)

    override fun generateModule(facet: MinecraftFacet): VelocityModule = VelocityModule(facet)
}
