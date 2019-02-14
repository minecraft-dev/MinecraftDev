/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getNodeIcon(isOpened: Boolean) = PlatformAssets.MINECRAFT_ICON
    override fun getName() = NAME
    override fun getDescription() = "Minecraft modules are used for developing plugins or mods for <b>Minecraft</b> (Java Edition, also known as the PC Edition)."

    companion object {
        private const val ID = "MINECRAFT_MODULE_TYPE"
        const val NAME = "Minecraft"

        val instance: MinecraftModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as MinecraftModuleType
    }
}
