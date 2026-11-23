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
import com.demonwav.mcdev.util.noVersionsFound
import com.intellij.openapi.diagnostic.logger

class NeoGradleVersion private constructor(val versions: List<SemanticVersion>) {

    companion object {
        private val LOGGER = logger<NeoGradleVersion>()

        /**
         * Returns the NeoGradle versions, or a failure after logging a warning if the download fails or finds none.
         */
        suspend fun downloadData(): Result<NeoGradleVersion> {
            val url = "https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/maven-metadata.xml"
            val versions = collectMavenVersions(url).getOrElse { return Result.failure(it) }.asSequence()
                .mapNotNull(SemanticVersion.Companion::tryParse)
                .sortedDescending()
                .take(50)
                .toList()
            if (versions.isEmpty()) {
                return noVersionsFound(LOGGER, "NeoGradle", url)
            }
            return Result.success(NeoGradleVersion(versions))
        }
    }
}
