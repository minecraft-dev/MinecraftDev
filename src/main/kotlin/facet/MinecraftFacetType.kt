/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        underlyingFacet: Facet<*>?,
    ) = MinecraftFacet(module, name, configuration, underlyingFacet)

    override fun createDefaultConfiguration() = MinecraftFacetConfiguration()
    override fun isSuitableModuleType(moduleType: ModuleType<*>?) = moduleType is JavaModuleType

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON

    companion object {
        const val TYPE_ID = "minecraft"
    }
}
