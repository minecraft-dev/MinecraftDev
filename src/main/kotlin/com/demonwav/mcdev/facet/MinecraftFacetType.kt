/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType

class MinecraftFacetType :
    FacetType<MinecraftFacet, MinecraftFacetConfiguration>(MinecraftFacet.ID, TYPE_ID, "Minecraft") {

    override fun createFacet(
        module: Module,
        name: String,
        configuration: MinecraftFacetConfiguration,
        underlyingFacet: Facet<*>?
    ) = MinecraftFacet(module, name, configuration, underlyingFacet)

    override fun createDefaultConfiguration() = MinecraftFacetConfiguration()
    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = moduleType is JavaModuleType

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON

    companion object {
        const val TYPE_ID = "minecraft"
    }
}
