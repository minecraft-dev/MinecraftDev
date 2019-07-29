/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

import com.demonwav.mcdev.platform.mcp.McpVersionPair
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.getMajorVersion
import com.demonwav.mcdev.util.sortVersions
import com.google.gson.Gson
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import kotlin.Int

class McpVersion private constructor(private val map: Map<String, Map<String, List<Int>>>) {

    val versions: List<String> by lazy {
        sortVersions(map.keys)
    }

    data class McpVersionSet(val goodVersions: List<McpVersionPair>, val badVersions: List<McpVersionPair>)

    private fun getSnapshot(version: String): McpVersionSet {
        return get(version, "snapshot")
    }

    private fun getStable(version: String): McpVersionSet {
        return get(version, "stable")
    }

    private operator fun get(version: String, type: String): McpVersionSet {
        val good = ArrayList<McpVersionPair>()
        val bad = ArrayList<McpVersionPair>()

        val keySet = map.keys
        for (mcVersion in keySet) {
            val versions = map[mcVersion]
            if (versions != null) {
                versions[type]?.let { vers ->
                    val pairs = vers.map { McpVersionPair("${type}_$it", mcVersion) }
                    if (mcVersion.startsWith(version)) {
                        good.addAll(pairs)
                    } else {
                        bad.addAll(pairs)
                    }
                }
            }
        }

        return McpVersionSet(good, bad)
    }

    fun getMcpVersionList(version: String): List<McpVersionEntry> {
        val result = mutableListOf<McpVersionEntry>()

        val majorVersion = getMajorVersion(version)

        val stable = getStable(majorVersion)
        stable.goodVersions.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry(s) }

        val snapshot = getSnapshot(majorVersion)
        snapshot.goodVersions.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry(s) }

        // The "seconds" in the pairs are bad, but still available to the user
        // We will color them read

        stable.badVersions.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry(s, true) }
        snapshot.badVersions.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry(s, true) }

        return result
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
