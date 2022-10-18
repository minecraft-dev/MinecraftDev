/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.version

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fromJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException

class ArchitecturyVersion private constructor(
    val versions: Map<SemanticVersion, List<SemanticVersion>>,
) {

    fun getArchitecturyVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        return try {
            val architecturyVersions = versions[mcVersion]
                ?: throw IOException("Could not find any architectury versions for $mcVersion")
            architecturyVersions.take(50)
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    data class ModrinthVersionApi(
        @SerializedName("version_number")
        val versionNumber: String,
        @SerializedName("game_versions")
        val gameVersions: List<String>,
    )

    companion object {

        suspend fun downloadData(): ArchitecturyVersion {
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
                    val parsed = SemanticVersion.parse(gameVersion)
                    val set = apiVersionMap.computeIfAbsent(parsed) { HashSet() }
                    set += apiVersion
                }
            }

            val apiVersionMapList = HashMap<SemanticVersion, List<SemanticVersion>>()
            for ((mcVersion, archList) in apiVersionMap.entries) {
                apiVersionMapList[mcVersion] = archList.sortedDescending()
            }

            return ArchitecturyVersion(apiVersionMapList)
        }
    }
}
