/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.util

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

data class ForgePackAdditionalData(
    val resourcePackFormat: Int? = null,
    val dataPackFormat: Int? = null,
    val serverDataPackFormat: Int? = null
) {
    companion object {
        val FORMAT_1_18 = ForgePackAdditionalData(8, 9)
        val FORMAT_1_19 = ForgePackAdditionalData(9, 10)
        val FORMAT_1_19_3 = ForgePackAdditionalData(12, 10)
        val FORMAT_1_19_4 = ForgePackAdditionalData(serverDataPackFormat = 12)

        fun forMcVersion(version: SemanticVersion): ForgePackAdditionalData? = when {
            version < MinecraftVersions.MC1_18 -> null
            version < MinecraftVersions.MC1_19 -> FORMAT_1_18
            version < MinecraftVersions.MC1_19_3 -> FORMAT_1_19
            version < MinecraftVersions.MC1_19_4 -> FORMAT_1_19_3
            else -> FORMAT_1_19_4
        }
    }
}
