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

package com.demonwav.mcdev.platform.architectury

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fromJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class ArchitecturyVersion private constructor(
    val versions: Map<SemanticVersion, List<SemanticVersion>>,
) {

    fun getArchitecturyVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        return versions[mcVersion].orEmpty().take(50)
    }

    data class ModrinthVersionApi(
        @SerializedName("version_number")
        val versionNumber: String,
        @SerializedName("game_versions")
        val gameVersions: List<String>,
    )

    companion object {

        suspend fun downloadData(): ArchitecturyVersion? {
            try {
                val url = "https://api.modrinth.com/v2/project/architectury-api/version"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val response = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .awaitString()

                val data = Gson().fromJson<List<ModrinthVersionApi>>(response)

                val apiVersionMap = HashMap<SemanticVersion, HashSet<SemanticVersion>>()

                for (version in data) {
                    val apiVersion = SemanticVersion.parse(version.versionNumber.substringBeforeLast('+'))

                    for (gameVersion in version.gameVersions) {
                        val parsed = SemanticVersion.tryParse(gameVersion) ?: continue
                        val set = apiVersionMap.computeIfAbsent(parsed) { HashSet() }
                        set += apiVersion
                    }
                }

                val apiVersionMapList = HashMap<SemanticVersion, List<SemanticVersion>>()
                for ((mcVersion, archList) in apiVersionMap.entries) {
                    apiVersionMapList[mcVersion] = archList.sortedDescending()
                }

                return ArchitecturyVersion(apiVersionMapList)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}
