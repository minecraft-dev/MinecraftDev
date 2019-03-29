/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.ProxyHttpConnectionFactory
import com.demonwav.mcdev.util.fromJson
import com.google.gson.Gson
import java.io.IOException
import java.util.Arrays
import java.util.Objects
import javax.swing.JComboBox

private const val cloudflareBaseUrl = "https://minecraftdev.org/versions/"
private const val githubBaseUrl = "https://raw.githubusercontent.com/minecraft-dev/minecraftdev.org/master/versions/"

fun getVersionSelector(type: PlatformType): PlatformVersion {
    val versionJson = type.versionJson ?: throw UnsupportedOperationException("Incorrect platform type: $type")

    return try {
        // attempt cloudflare
        doCall(cloudflareBaseUrl + versionJson)
    } catch (e: IOException) {
        // if that fails, attempt github
        doCall(githubBaseUrl + versionJson)
    }
}

private fun doCall(urlText: String): PlatformVersion {
    val connection = ProxyHttpConnectionFactory.openHttpConnection(urlText)

    connection.setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"
    )

    val text = connection.inputStream.use { stream -> stream.reader().use { it.readText() } }
    return Gson().fromJson(text)
}

data class PlatformVersion(var versions: Array<String>, var selectedIndex: Int) {

    fun set(combo: JComboBox<String>) {
        combo.removeAllItems()
        for (version in this.versions) {
            combo.addItem(version)
        }
        combo.selectedIndex = this.selectedIndex
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is PlatformVersion) {
            return false
        }

        return Arrays.equals(this.versions, other.versions) && this.selectedIndex == other.selectedIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(*versions, selectedIndex)
    }
}
