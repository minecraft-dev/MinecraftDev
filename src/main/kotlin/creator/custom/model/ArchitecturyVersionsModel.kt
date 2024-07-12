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

package com.demonwav.mcdev.creator.custom.model

import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion

@TemplateApi
data class ArchitecturyVersionsModel(
    val minecraft: SemanticVersion,
    val forge: SemanticVersion?,
    val neoforge: SemanticVersion?,
    val loom: SemanticVersion,
    val loader: SemanticVersion,
    val yarn: FabricVersions.YarnVersion,
    val useFabricApi: Boolean,
    val fabricApi: SemanticVersion,
    val useOfficialMappings: Boolean,
    val useArchitecturyApi: Boolean,
    val architecturyApi: SemanticVersion,
) : HasMinecraftVersion {

    override val minecraftVersion: SemanticVersion = minecraft

    val minecraftNext by lazy {
        val mcNext = when (val part = minecraft.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        "1.$mcNext"
    }

    val hasForge: Boolean by lazy { !forge?.parts.isNullOrEmpty() }
    val forgeSpec: String? by lazy { forge?.parts?.getOrNull(0)?.versionString }

    val hasNeoforge: Boolean by lazy { !neoforge?.parts.isNullOrEmpty() }
    val neoforgeSpec: String? by lazy { neoforge?.parts?.getOrNull(0)?.versionString }
}
