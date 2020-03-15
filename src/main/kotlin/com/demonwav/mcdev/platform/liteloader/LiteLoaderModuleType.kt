/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType

object LiteLoaderModuleType : AbstractModuleType<LiteLoaderModule>("", "") {

    private const val ID = "LITELOADER_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = emptyList<String>()
    val LISTENER_ANNOTATIONS = emptyList<String>()

    override val platformType = PlatformType.LITELOADER
    override val icon = PlatformAssets.LITELOADER_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = LiteLoaderModule(facet)
}
