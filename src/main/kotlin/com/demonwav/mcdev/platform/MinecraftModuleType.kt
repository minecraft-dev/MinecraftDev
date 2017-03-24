/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.creator.MinecraftModuleBuilder
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleTypeManager

class MinecraftModuleType : JavaModuleType() {

    override fun createModuleBuilder() = MinecraftModuleBuilder()
    override fun getBigIcon() = PlatformAssets.MINECRAFT_ICON_2X
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getNodeIcon(isOpened: Boolean) = PlatformAssets.MINECRAFT_ICON

    companion object {
        private val ID = "MINECRAFT_MODULE_TYPE"
        val OPTION = "com.demonwav.mcdev.MinecraftModuleTypes"

        val instance: MinecraftModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as MinecraftModuleType
    }
}
