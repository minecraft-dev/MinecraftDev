/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.util.ProxyHttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import java.util.LinkedHashMap
import javax.swing.JComboBox

private const val spongeUrl = "https://minecraftdev.org/versions/sponge_v2.json"

data class SpongeVersion(var versions: LinkedHashMap<String, String>, var selectedIndex: Int) {

    fun set(combo: JComboBox<String>) {
        combo.removeAllItems()
        for ((key, _) in this.versions) {
            combo.addItem(key)
        }
        combo.selectedIndex = this.selectedIndex
    }

    companion object {
        fun downloadData(): SpongeVersion? {
            val connection = ProxyHttpConnectionFactory.openHttpConnection(spongeUrl)

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"
            )

            return try {
                val text = connection.inputStream.use { stream -> stream.reader().use { it.readText() } }
                Gson().fromJson(text)
            } catch (e: Exception) {
                null
            }
        }
    }
}
