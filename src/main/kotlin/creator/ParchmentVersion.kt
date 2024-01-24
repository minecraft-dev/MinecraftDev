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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.SemanticVersion
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.intellij.openapi.diagnostic.logger

class ParchmentVersion private constructor(val mcVersion: SemanticVersion, val parchmentVersion: String) {

    private val presentableString by lazy { "$mcVersion - $parchmentVersion" }

    override fun toString(): String = presentableString

    companion object {
        private val LOGGER = logger<ParchmentVersion>()

        suspend fun downloadData(limit: Int = 50): List<ParchmentVersion> {
            val versions = mutableListOf<ParchmentVersion>()
            val mcVersions = collectSupportedMcVersions() ?: return emptyList()
            for (mcVersion in mcVersions) {
                val url = "https://maven.parchmentmc.org/org/parchmentmc/data/parchment-$mcVersion/maven-metadata.xml"
                try {
                    collectMavenVersions(url)
                        .mapTo(versions) { ParchmentVersion(mcVersion, it) }
                    if (versions.size > limit) {
                        return versions.subList(0, limit)
                    }
                } catch (e: Exception) {
                    if (e !is FuelError || e.exception !is HttpException) {
                        LOGGER.error("Failed to retrieve Parchment version data from $url", e)
                    }
                }
            }

            return versions
        }

        private suspend fun collectSupportedMcVersions(): List<SemanticVersion>? {
            try {
                val baseUrl = "https://maven.parchmentmc.org/org/parchmentmc/data/"
                val scrapeArtifactoryDirectoryListing = scrapeArtifactoryDirectoryListing(baseUrl)
                return scrapeArtifactoryDirectoryListing
                    .asReversed()
                    .asSequence()
                    .filter { it.startsWith("parchment-") }
                    .mapNotNull {
                        val mcVersion = it.removePrefix("parchment-").removeSuffix("/")
                        SemanticVersion.tryParse(mcVersion)
                    }
                    .toList()
            } catch (e: Exception) {
                if (e is FuelError && e.exception is HttpException) {
                    return null
                }
                LOGGER.error("Failed to list supported Parchment Minecraft versions", e)
            }

            return null
        }
    }
}
