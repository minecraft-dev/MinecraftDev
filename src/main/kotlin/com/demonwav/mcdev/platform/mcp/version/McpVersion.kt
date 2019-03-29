/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

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

    private fun getSnapshot(version: String): Pair<List<Int>, List<Int>> {
        return get(version, "snapshot")
    }

    private fun getStable(version: String): Pair<List<Int>, List<Int>> {
        return get(version, "stable")
    }

    private operator fun get(version: String, type: String): Pair<List<Int>, List<Int>> {
        val good = ArrayList<Int>()
        val bad = ArrayList<Int>()

        val keySet = map.keys
        for (key in keySet) {
            val versions = map[key]
            if (versions != null) {
                if (key.startsWith(version)) {
                    good.addAll(versions[type]!!.map(Int::toInt))
                } else {
                    bad.addAll(versions[type]!!.map(Int::toInt))
                }
            }
        }

        return good to bad
    }

    fun getMcpVersionList(version: String): List<McpVersionEntry> {
        val result = mutableListOf<McpVersionEntry>()

        val majorVersion = getMajorVersion(version)

        val stable = getStable(majorVersion)
        stable.first.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry("stable_$s") }

        val snapshot = getSnapshot(majorVersion)
        snapshot.first.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry("snapshot_$s") }

        // The "seconds" in the pairs are bad, but still available to the user
        // We will color them read

        stable.second.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry("stable_$s", true) }
        snapshot.second.asSequence().sortedWith(Comparator.reverseOrder())
            .mapTo(result) { s -> McpVersionEntry("snapshot_$s", true) }

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
            } catch (ignored: IOException) {}

            return null
        }
    }
}
