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

package com.demonwav.mcdev.platform.fabric.util

import com.demonwav.mcdev.util.McdevHttp
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fetchCatching
import com.demonwav.mcdev.util.noVersionsFound
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

private val LOGGER = logger<FabricApiVersions>()

class FabricApiVersions(val versions: List<Version>) {
    class Version(val gameVersions: List<String>, val version: SemanticVersion)

    companion object {
        private const val URL = "https://api.modrinth.com/v2/project/P7dR8mSH/version"

        /**
         * Returns the Fabric API versions, or a failure after logging a warning if the download fails or finds none.
         */
        suspend fun downloadData(): Result<FabricApiVersions> {
            val versions = fetchCatching(LOGGER, URL) {
                parse(McdevHttp.getString(URL)) ?: throw IOException("Unexpected Fabric API version data")
            }.getOrElse { return Result.failure(it) }
            if (versions.versions.isEmpty()) {
                return noVersionsFound(LOGGER, "Fabric API", URL)
            }
            return Result.success(versions)
        }

        private fun parse(text: String): FabricApiVersions? {
            val versions = mutableListOf<Version>()
            val json = JsonParser.parseString(text)?.asJsonArray ?: return null
            versionLoop@
            for (ver in json) {
                val version = ver?.asJsonObject ?: return null
                val files = version["files"]?.asJsonArray ?: return null
                for (file in files) {
                    val fileObj = file?.asJsonObject ?: return null
                    val filename = fileObj["filename"]?.asString ?: return null
                    if (!filename.startsWith("fabric-api-")) {
                        continue@versionLoop
                    }
                }
                val versionNumber = version["version_number"]?.asString?.let(SemanticVersion::tryParse)
                    ?: return null
                val gameVersions = version["game_versions"]?.asJsonArray ?: return null
                val gameVersionsList = mutableListOf<String>()
                for (gameVer in gameVersions) {
                    val gameVersion = gameVer?.asString ?: return null
                    gameVersionsList += gameVersion
                }
                versions += Version(gameVersionsList, versionNumber)
            }
            return FabricApiVersions(versions)
        }
    }
}
