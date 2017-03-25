/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.version

import com.demonwav.mcdev.util.sortVersions
import com.google.common.base.Objects
import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.net.URL
import java.util.ArrayList

class ForgeVersion private constructor() {

    private var map: Map<*, *> = mutableMapOf<Any, Any>()

    val sortedMcVersions: List<String>
        get() {
            val mcversion = map["mcversion"] as Map<*, *>
            return sortVersions(mcversion.keys)
        }

    fun getRecommended(versions: List<String>): String? {
        var recommended = "1.7"
        for (version in versions) {
            getPromo(version) ?: continue

            if (recommended < version) {
                recommended = version
            }
        }

        return recommended
    }

    fun getPromo(version: String): Double? {
        val promos = map["promos"] as? Map<*, *>
        if (promos != null) {
            return promos[version + "-recommended"] as Double
        }
        return null
    }

    fun getForgeVersions(version: String): List<String> {
        val list = ArrayList<String>()
        val numbers = map["number"] as? Map<*, *>
        numbers?.forEach { _, v ->
            if (v is Map<*, *>) {
                val number = v
                val currentVersion = number["mcversion"] as String

                if (Objects.equal(currentVersion, version)) {
                    list.add(number["version"] as String)
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

        if (branch == null) {
            return mcVersion + "-" + finalVersion
        } else {
            return "$mcVersion-$finalVersion-$branch"
        }
    }

    companion object {
        fun downloadData(): ForgeVersion? {
            try {
                URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/json").openStream().use { inputStream ->
                    val text = IOUtils.toString(inputStream)

                    val map = Gson().fromJson(text, Map::class.java)
                    val version = ForgeVersion()
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
