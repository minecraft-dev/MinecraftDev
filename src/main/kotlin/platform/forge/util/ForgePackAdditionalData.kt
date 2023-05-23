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
