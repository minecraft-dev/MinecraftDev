/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
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
import java.util.ArrayList
import kotlin.Int
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
            try {
                val text = URL("http://export.mcpbot.bspk.rs/versions.json").readText()
                val map = Gson().fromJson<Map<String, Map<String, List<Int>>>>(text)
                val mcpVersion = McpVersion(map)
                mcpVersion.versions
                return mcpVersion
            } catch (ignored: IOException) {
            }

            return null
        }
    }
}
