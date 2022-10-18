/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.version

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.sortVersions
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.coroutines.awaitString
import com.google.gson.Gson
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException

class LiteLoaderVersion private constructor(private var map: Map<*, *>) {

    val sortedMcVersions: List<SemanticVersion> by lazy {
        val mcVersion = map["versions"] as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        val keys = mcVersion.keys as Collection<String>
        return@lazy sortVersions(keys)
    }

    companion object {
        private val LOGGER = Logger.getInstance(LiteLoaderVersion::class.java)

        suspend fun downloadData(): LiteLoaderVersion? {
            try {
                val url = "https://dl.liteloader.com/versions/versions.json"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val text = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .awaitString()

                val map = Gson().fromJson<Map<*, *>>(text)
                val liteLoaderVersion = LiteLoaderVersion(map)
                liteLoaderVersion.sortedMcVersions
                return liteLoaderVersion
            } catch (e: IOException) {
                LOGGER.error("Failed to download LiteLoader version json", e)
            }
            return null
        }
    }
}
