/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.version

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.sortVersions
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import java.io.IOException
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

class ForgeVersion private constructor(val versions: List<String>) {

    val sortedMcVersions: List<SemanticVersion> by lazy {
        val unsortedVersions = versions.asSequence()
            .mapNotNull(
                fun(version: String): String? {
                    val index = version.indexOf('-')
                    if (index == -1) {
                        return null
                    }
                    return version.substring(0, index)
                }
            ).distinct()
            .toList()
        return@lazy sortVersions(unsortedVersions)
    }

    fun getForgeVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        val versionText = mcVersion.toString()
        return versions.asSequence()
            .filter { it.substringBefore('-') == versionText }
            .mapNotNull {
                try {
                    SemanticVersion.parse(it.substringAfter('-'))
                } catch (ignore: Exception) {
                    null
                }
            }
            .sortedDescending()
            .take(50)
            .toList()
    }

    companion object {
        suspend fun downloadData(): ForgeVersion? {
            try {
                val url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val response = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .await()

                val result = mutableListOf<String>()
                response.body().toStream().use { stream ->
                    val inputFactory = XMLInputFactory.newInstance()

                    @Suppress("UNCHECKED_CAST")
                    val reader = inputFactory.createXMLEventReader(stream) as Iterator<XMLEvent>
                    for (event in reader) {
                        if (!event.isStartElement) {
                            continue
                        }
                        val start = event.asStartElement()
                        val name = start.name.localPart
                        if (name != "version") {
                            continue
                        }

                        val versionEvent = reader.next()
                        if (!versionEvent.isCharacters) {
                            continue
                        }
                        val version = versionEvent.asCharacters().data
                        val index = version.indexOf('-')
                        if (index == -1) {
                            continue
                        }

                        result += version
                    }
                }

                return ForgeVersion(result)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}
