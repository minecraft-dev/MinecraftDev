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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fetchOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.HttpRequests

class ParchmentVersion private constructor(val mcVersion: SemanticVersion, val parchmentVersion: String) {

    private val presentableString by lazy { "$mcVersion - $parchmentVersion" }

    override fun toString(): String = presentableString

    companion object {
        private val LOGGER = logger<ParchmentVersion>()

        private const val BASE_URL = "https://maven.parchmentmc.org/org/parchmentmc/data/"

        /**
         * Returns up to [limit] Parchment versions, newest Minecraft version first, or null after logging a warning if
         * none could be downloaded. A Minecraft version whose versions fail to download is left out with a warning.
         */
        suspend fun downloadData(limit: Int = 50): List<ParchmentVersion>? {
            val versions = mutableListOf<ParchmentVersion>()
            val mcVersions = collectSupportedMcVersions() ?: return null
            for (mcVersion in mcVersions) {
                val url = "${BASE_URL}parchment-$mcVersion/maven-metadata.xml"
                val found = fetchOrNull(LOGGER, url) {
                    try {
                        fetchMavenVersions(url)
                    } catch (e: HttpRequests.HttpStatusException) {
                        // Some listed Minecraft versions have no Parchment release
                        if (e.statusCode != 404) {
                            throw e
                        }
                        null
                    }
                } ?: continue
                found.mapTo(versions) { ParchmentVersion(mcVersion, it) }
                if (versions.size >= limit) {
                    return versions.subList(0, limit)
                }
            }

            if (versions.isEmpty()) {
                LOGGER.warn("No Parchment versions found at $BASE_URL")
                return null
            }
            return versions
        }

        private suspend fun collectSupportedMcVersions(): List<SemanticVersion>? {
            val listing = scrapeArtifactoryDirectoryListing(BASE_URL) ?: return null
            return listing
                .asSequence()
                .filter { it.startsWith("parchment-") }
                .mapNotNull {
                    val mcVersion = it.removePrefix("parchment-").removeSuffix("/")
                    SemanticVersion.tryParse(mcVersion)
                }
                .sortedDescending()
                .toList()
        }
    }
}
