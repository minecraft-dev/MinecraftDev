/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

import com.demonwav.mcdev.platform.mcp.McpVersionPair
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.sortVersions
import com.google.gson.Gson
import java.io.IOException
import java.net.URL
import kotlin.math.min

class McpVersion private constructor(private val map: Map<String, Map<String, List<Int>>>) {

    val versions: List<SemanticVersion> by lazy {
        sortVersions(map.keys)
    }

    data class McpVersionSet(val goodVersions: List<McpVersionPair>, val badVersions: List<McpVersionPair>)

    private fun getSnapshot(version: SemanticVersion): McpVersionSet {
        return get(version, "snapshot")
    }

    private fun getStable(version: SemanticVersion): McpVersionSet {
        return get(version, "stable")
    }

    private operator fun get(version: SemanticVersion, type: String): McpVersionSet {
        val good = ArrayList<McpVersionPair>()
        val bad = ArrayList<McpVersionPair>()

        val keySet = map.keys
        for (mcVersion in keySet) {
            val versions = map[mcVersion]
            if (versions != null) {
                versions[type]?.let { vers ->
                    val mcVersionParsed = SemanticVersion.parse(mcVersion)
                    val pairs = vers.map { McpVersionPair("${type}_$it", mcVersionParsed) }
                    if (mcVersionParsed.startsWith(version)) {
                        good.addAll(pairs)
                    } else {
                        bad.addAll(pairs)
                    }
                }
            }
        }

        return McpVersionSet(good, bad)
    }

    fun getMcpVersionList(version: SemanticVersion): List<McpVersionEntry> {
        val limit = 50

        val result = ArrayList<McpVersionEntry>(limit * 4)

        val majorVersion = version.take(2)
        val stable = getStable(majorVersion)
        val snapshot = getSnapshot(majorVersion)

        fun mapTopTo(source: List<McpVersionPair>, dest: MutableList<McpVersionEntry>, limit: Int, isRed: Boolean) {
            val tempList = ArrayList(source).apply { sortDescending() }
            for (i in 0 until min(limit, tempList.size)) {
                dest += McpVersionEntry(tempList[i], isRed)
            }
        }

        mapTopTo(stable.goodVersions, result, limit, false)
        mapTopTo(snapshot.goodVersions, result, limit, false)

        // If we're already at the limit we don't need to go through the bad list
        if (result.size >= limit) {
            return result.subList(0, min(limit, result.size))
        }

        // The bad pairs don't match the current MC version, but are still available to the user
        // We will color them red
        mapTopTo(stable.badVersions, result, limit, true)
        mapTopTo(snapshot.badVersions, result, limit, true)

        return result.subList(0, min(limit, result.size))
    }

    companion object {
        fun downloadData(): McpVersion? {
            val bspkrsMappings = try {
                val bspkrsText = URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/versions.json").readText()
                Gson().fromJson<MutableMap<String, MutableMap<String, MutableList<Int>>>>(bspkrsText)
            } catch (ignored: IOException) {
                mutableMapOf()
            }

            val tterragMappings = try {
                val tterragText = URL("https://assets.tterrag.com/temp_mappings.json").readText()
                Gson().fromJson<MutableMap<String, MutableMap<String, MutableList<Int>>>>(tterragText)
            } catch (ignored: IOException) {
                emptyMap()
            }

            // Merge the temporary mappings list into the main one, temporary solution for 1.16
            tterragMappings.forEach { (mcVersion, channels) ->
                val existingChannels = bspkrsMappings.getOrPut(mcVersion, ::mutableMapOf)
                channels.forEach { (channelName, newVersions) ->
                    val existingVersions = existingChannels.getOrPut(channelName, ::mutableListOf)
                    existingVersions.addAll(newVersions)
                }
            }

            return if (bspkrsMappings.isEmpty()) null else McpVersion(bspkrsMappings)
        }
    }
}
