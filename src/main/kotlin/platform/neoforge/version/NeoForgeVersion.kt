/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.neoforge.version

import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

class NeoForgeVersion private constructor(val versions: List<String>) {

    private val versionsByMc: Map<SemanticVersion, List<String>> by lazy {
        versions.groupBy { getMinecraftVersion(it)?.let(SemanticVersion::tryParse) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    val sortedMcVersions: List<SemanticVersion> by lazy {
        versionsByMc.keys.sortedDescending()
    }

    fun getNeoForgeVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        return versionsByMc[mcVersion].orEmpty().asSequence()
            .mapNotNull(SemanticVersion::tryParse)
            .sortedDescending()
            .take(50)
            .toList()
    }

    companion object {
        private val LOGGER = logger<NeoForgeVersion>()

        /**
         * Returns the Minecraft version a NeoForge version targets, or `null` if it can't be determined.
         *
         * Before Minecraft 26.1, NeoForge versions dropped the leading `1.` of the Minecraft version
         * (e.g. `21.1.77` -> `1.21.1`). Since 26.1, NeoForge versions start with the full Minecraft version,
         * always including the patch part (e.g. `26.1.0.19-beta` -> `26.1`, `26.1.1.6-beta` -> `26.1.1`).
         */
        fun getMinecraftVersion(neoForgeVersion: String): String? {
            val base = neoForgeVersion.substringBeforeLast('.')
            val major = base.substringBefore('.').toIntOrNull() ?: return null
            return if (major >= 26) {
                if (base.count { it == '.' } != 2) {
                    return null
                }
                base.removeSuffix(".0")
            } else {
                "1." + base.removeSuffix(".0")
            }
        }

        suspend fun downloadData(): NeoForgeVersion? {
            try {
                val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
                val versions = collectMavenVersions(url)
                return NeoForgeVersion(versions)
            } catch (e: IOException) {
                LOGGER.error("Failed to retrieve NeoForge version data", e)
            }
            return null
        }
    }
}
