/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

import com.demonwav.mcdev.util.sortVersions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.util.Pair
import org.apache.commons.io.IOUtils
import java.awt.event.ActionListener
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JComboBox

class McpVersion private constructor() {

    private var map: Map<String, Map<String, List<Int>>> = HashMap()

    val versions: List<String>
        get() = sortVersions(map.keys)

    fun getSnapshot(version: String): Pair<List<Int>, List<Int>> {
        return get(version, "snapshot")
    }

    fun getStable(version: String): Pair<List<Int>, List<Int>> {
        return get(version, "stable")
    }

    private operator fun get(version: String, type: String): Pair<List<Int>, List<Int>> {
        val good = ArrayList<Int>()
        val bad = ArrayList<Int>()

        val keySet = map.keys
        for (key in keySet) {
            val versions = map[key]
            if (versions != null) {
                if (key == version) {
                    good.addAll(versions[type]!!.map(Int::toInt))
                } else {
                    bad.addAll(versions[type]!!.map(Int::toInt))
                }
            }
        }

        return Pair(good, bad)
    }

    fun setMcpVersion(mcpVersionBox: JComboBox<McpVersionEntry>, version: String, actionListener: ActionListener) {
        mcpVersionBox.removeActionListener(actionListener)
        mcpVersionBox.removeAllItems()

        val stable = getStable(version)
        stable.getFirst().stream().sorted { one, two -> one!!.compareTo(two!!) * -1 }
            .map { s -> McpVersionEntry("stable_" + s!!) }.forEach { mcpVersionBox.addItem(it) }

        val snapshot = getSnapshot(version)
        snapshot.getFirst().stream().sorted { one, two -> one!!.compareTo(two!!) * -1 }
            .map { s -> McpVersionEntry("snapshot_" + s!!) }.forEach { mcpVersionBox.addItem(it) }

        // The "seconds" in the pairs are bad, but still available to the user
        // We will color them read

        stable.getSecond().stream().sorted { one, two -> one!!.compareTo(two!!) * -1 }
            .map { s -> McpVersionEntry("stable_" + s!!, true) }.forEach { mcpVersionBox.addItem(it) }
        snapshot.getSecond().stream().sorted { one, two -> one!!.compareTo(two!!) * -1 }
            .map { s -> McpVersionEntry("snapshot_" + s!!, true) }.forEach { mcpVersionBox.addItem(it) }

        mcpVersionBox.addActionListener(actionListener)
    }

    companion object {
        fun downloadData(): McpVersion? {
            try {
                URL("http://export.mcpbot.bspk.rs/versions.json").openStream().use { inStream ->
                    val text = IOUtils.toString(inStream)

                    val tokenType = object : TypeToken<Map<String, Map<String, List<Int>>>>() {

                    }.type
                    val map = Gson().fromJson<Map<String, Map<String, List<Int>>>>(text, tokenType)
                    val version = McpVersion()
                    version.map = map
                    return version
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return null
        }
    }
}
