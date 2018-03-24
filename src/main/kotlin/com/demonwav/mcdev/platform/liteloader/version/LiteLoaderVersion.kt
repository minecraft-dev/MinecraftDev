/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.version

import com.demonwav.mcdev.util.fromJson
import com.demonwav.mcdev.util.gson
import com.demonwav.mcdev.util.sortVersions
import java.io.IOException
import java.net.URL

class LiteLoaderVersion private constructor(private var map: Map<*, *>) {

    val sortedMcVersions: List<String> by lazy {
        val mcVersion = map["versions"] as Map<*, *>
        sortVersions(mcVersion.keys)
    }

    companion object {
        fun downloadData(): LiteLoaderVersion? {
            try {
                val text = URL("http://dl.liteloader.com/versions/versions.json").readText()

                val map = gson.fromJson<Map<*, *>>(text)
                val liteLoaderVersion = LiteLoaderVersion(map)
                liteLoaderVersion.sortedMcVersions
                return liteLoaderVersion
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}
