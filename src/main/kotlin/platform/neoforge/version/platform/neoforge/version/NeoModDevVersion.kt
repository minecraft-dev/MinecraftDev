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

package com.demonwav.mcdev.platform.neoforge.version.platform.neoforge.version

import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

class NeoModDevVersion private constructor(val versions: List<SemanticVersion>) {

    companion object {
        private val LOGGER = logger<NeoModDevVersion>()

        suspend fun downloadData(): NeoModDevVersion? {
            try {
                val url = "https://maven.neoforged.net/releases/net/neoforged/moddev" +
                    "/net.neoforged.moddev.gradle.plugin/maven-metadata.xml"
                val versions = collectMavenVersions(url)
                    .asSequence()
                    .mapNotNull(SemanticVersion.Companion::tryParse)
                    .sortedDescending()
                    .take(50)
                    .toList()
                return NeoModDevVersion(versions)
            } catch (e: IOException) {
                LOGGER.error("Failed to retrieve NeoForged ModDev version data", e)
            }
            return null
        }
    }
}
