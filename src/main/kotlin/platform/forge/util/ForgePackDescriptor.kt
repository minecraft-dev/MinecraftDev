/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
                " which requires language files to have uppercase letters (eg: en_US.lang)."
        )
        val FORMAT_4 = ForgePackDescriptor(
            4,
            "A pack_format of 4 requires json lang files. Note: we require v4 pack meta for all mods."
        )
        val FORMAT_5 = ForgePackDescriptor(
            5,
            "A pack_format of 5 requires json lang files and some texture changes from 1.15." +
                " Note: we require v5 pack meta for all mods."
        )
        val FORMAT_6 = ForgePackDescriptor(
            6,
            "A pack_format of 6 requires json lang files and some texture changes from 1.16.2." +
                " Note: we require v6 pack meta for all mods."
        )
        val FORMAT_7 = ForgePackDescriptor(7, "")
        val FORMAT_8 = ForgePackDescriptor(8, "")
        val FORMAT_9 = ForgePackDescriptor(9, "")

        // See https://minecraft.gamepedia.com/Tutorials/Creating_a_resource_pack#.22pack_format.22
        fun forMcVersion(version: SemanticVersion): ForgePackDescriptor? = when {
            version <= MinecraftVersions.MC1_12_2 -> FORMAT_3
            version <= MinecraftVersions.MC1_14_4 -> FORMAT_4
            version <= MinecraftVersions.MC1_16_1 -> FORMAT_5
            version < MinecraftVersions.MC1_17 -> FORMAT_6
            version < MinecraftVersions.MC1_18 -> FORMAT_7
            version < MinecraftVersions.MC1_19 -> FORMAT_8
            version >= MinecraftVersions.MC1_19 -> FORMAT_9
            else -> null
        }
    }
}
