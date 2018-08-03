/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.util.ProxyHttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.gson
import org.jetbrains.concurrency.runAsync
import java.util.Objects
import javax.swing.JComboBox

private const val spongeUrl = "https://minecraftdev.org/versions/sponge_v2.json"

fun getSpongeVersionSelector() = runAsync {
    SpongeVersion.downloadData()
}

data class SpongeVersion(var versions: LinkedHashMap<String, String>, var selectedIndex: Int) {

    fun set(combo: JComboBox<String>) {
        combo.removeAllItems()
        for ((key, _) in this.versions) {
            combo.addItem(key)
        }
        combo.selectedIndex = this.selectedIndex
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is SpongeVersion) {
            return false
        }

        return this.versions == other.versions && this.selectedIndex == other.selectedIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(versions, selectedIndex)
    }

    companion object {
        fun downloadData(): SpongeVersion {
            val connection = ProxyHttpConnectionFactory.openHttpConnection(spongeUrl)

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"
            )

            val text = connection.inputStream.use { stream -> stream.reader().use { it.readText() } }
            return gson.fromJson(text)
        }
    }
}
