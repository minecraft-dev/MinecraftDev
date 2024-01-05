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

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.intellij.openapi.diagnostic.logger

class ParchmentVersion private constructor(val versions: List<String>) {
    companion object {
        private val LOGGER = logger<ParchmentVersion>()

        suspend fun downloadData(mcVersion: String): ParchmentVersion? {
            try {
                // Using this URL doesn't work currently
                // val url = "https://maven.parchmentmc.org/org/parchmentmc/parchment-$mcVersion/maven-metadata.xml"
                val url = "https://ldtteam.jfrog.io/artifactory/parchmentmc-public/org/parchmentmc/data/parchment-$mcVersion/maven-metadata.xml"
                val versions = collectMavenVersions(url)
                return ParchmentVersion(versions)
            } catch (e: Exception) {
                if (e is FuelError && e.exception is HttpException) {
                    return null
                }
                LOGGER.error("Failed to retrieve Parchment $mcVersion version data", e)
            }
            return null
        }
    }
}
