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

package com.demonwav.mcdev.platform.forge.util

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

data class ForgePackDescriptor(val format: Int, val comment: String) {
    companion object {
        // Current data from https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/mdk/src/main/resources/pack.mcmeta
        val FORMAT_3 = ForgePackDescriptor(
            3,
            "A pack_format of 3 should be used starting with Minecraft 1.11." +
                " All resources, including language files, should be lowercase (eg: en_us.lang)." +
                " A pack_format of 2 will load your mod resources with LegacyV2Adapter," +
                " which requires language files to have uppercase letters (eg: en_US.lang).",
        )
        val FORMAT_4 = ForgePackDescriptor(
            4,
            "A pack_format of 4 requires json lang files. Note: we require v4 pack meta for all mods.",
        )
        val FORMAT_5 = ForgePackDescriptor(
            5,
            "A pack_format of 5 requires json lang files and some texture changes from 1.15." +
                " Note: we require v5 pack meta for all mods.",
        )
        val FORMAT_6 = ForgePackDescriptor(
            6,
            "A pack_format of 6 requires json lang files and some texture changes from 1.16.2." +
                " Note: we require v6 pack meta for all mods.",
        )
        val FORMAT_7 = ForgePackDescriptor(7, "")
        val FORMAT_8 = ForgePackDescriptor(8, "")
        val FORMAT_9 = ForgePackDescriptor(9, "")
        val FORMAT_10 = ForgePackDescriptor(10, "")
        val FORMAT_12 = ForgePackDescriptor(12, "")
        val FORMAT_15 = ForgePackDescriptor(15, "")
        val FORMAT_18 = ForgePackDescriptor(18, "")

        // See https://minecraft.gamepedia.com/Tutorials/Creating_a_resource_pack#.22pack_format.22
        fun forMcVersion(version: SemanticVersion): ForgePackDescriptor? = when {
            version <= MinecraftVersions.MC1_12_2 -> FORMAT_3
            version <= MinecraftVersions.MC1_14_4 -> FORMAT_4
            version <= MinecraftVersions.MC1_16_1 -> FORMAT_5
            version < MinecraftVersions.MC1_17 -> FORMAT_6
            version < MinecraftVersions.MC1_18 -> FORMAT_7
            version < MinecraftVersions.MC1_18_2 -> FORMAT_8
            version < MinecraftVersions.MC1_19 -> FORMAT_9
            version < MinecraftVersions.MC1_19_3 -> FORMAT_10
            version < MinecraftVersions.MC1_20 -> FORMAT_12
            version < MinecraftVersions.MC1_20_2 -> FORMAT_15
            version >= MinecraftVersions.MC1_20_2 -> FORMAT_18
            else -> null
        }
    }
}
