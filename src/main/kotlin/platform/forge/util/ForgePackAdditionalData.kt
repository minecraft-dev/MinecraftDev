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

data class ForgePackAdditionalData(val resourcePackFormat: Int, val dataPackFormat: Int) {
    companion object {
        val FORMAT_1_18 = ForgePackAdditionalData(8, 9)
        val FORMAT_1_19 = ForgePackAdditionalData(9, 10)

        fun forMcVersion(version: SemanticVersion): ForgePackAdditionalData? = when {
            version < MinecraftVersions.MC1_18 -> null
            version < MinecraftVersions.MC1_19 -> FORMAT_1_18
            else -> FORMAT_1_19
        }
    }
}
