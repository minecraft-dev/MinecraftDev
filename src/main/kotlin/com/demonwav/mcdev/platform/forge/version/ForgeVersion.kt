/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.version

import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.gson
import com.demonwav.mcdev.util.sortVersions
import java.io.IOException
import java.net.URL
import java.util.ArrayList

class ForgeVersion private constructor(private val map: Map<*, *>) {

    val sortedMcVersions: List<String> by lazy {
        sortVersions((map["mcversion"] as Map<*, *>).keys)
    }

    fun getRecommended(versions: List<String>): String {
        var recommended = SemanticVersion.release(1, 7)
        for (version in versions) {
            getPromo(version) ?: continue
            val semantic = SemanticVersion.parse(version)
            if (recommended < semantic) {
                recommended = semantic
            }
        }

        return recommended.versionString
    }

    fun getPromo(version: String): Double? {
        val promos = map["promos"] as? Map<*, *>
        if (promos != null) {
            return promos[version + "-recommended"] as? Double
        }
        return null
    }

    fun getForgeVersions(version: String): List<String> {
        val list = ArrayList<String>()
        val numbers = map["number"] as? Map<*, *>
        numbers?.forEach { _, number ->
            if (number is Map<*, *>) {
                val currentVersion = number["mcversion"] as? String

                if (currentVersion == version) {
                    list.add(number["version"] as? String ?: return@forEach)
                }
            }
        }
        return list
    }

    fun getFullVersion(version: String): String? {
        val numbers = map["number"] as? Map<*, *> ?: return null
        val parts = version.split(".").dropLastWhile(String::isEmpty).toTypedArray()
        val versionSmall = parts.last()
        val number = numbers[versionSmall] as? Map<*, *> ?: return null

        val branch = number["branch"] as? String
        val mcVersion = number["mcversion"] as? String ?: return null
        val finalVersion = number["version"] as? String ?: return null

        return if (branch == null) {
            "$mcVersion-$finalVersion"
        } else {
            "$mcVersion-$finalVersion-$branch"
        }
    }

    companion object {
        fun downloadData(): ForgeVersion? {
            try {
                val text = URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/json").readText()

                val map = gson.fromJson(text, Map::class.java)
                val forgeVersion = ForgeVersion(map)
                forgeVersion.sortedMcVersions // sort em up
                return forgeVersion
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}
