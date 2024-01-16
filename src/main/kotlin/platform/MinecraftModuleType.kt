/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleTypeManager

class MinecraftModuleType : JavaModuleType() {

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getNodeIcon(isOpened: Boolean) = PlatformAssets.MINECRAFT_ICON
    override fun getName() = NAME
    override fun getDescription() =
        "Minecraft modules are used for developing plugins or mods for <b>Minecraft</b> " +
            "(Java Edition, also known as the PC Edition)."

    companion object {
        private const val ID = "MINECRAFT_MODULE_TYPE"
        const val NAME = "Minecraft"

        val instance: MinecraftModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as MinecraftModuleType
    }
}
