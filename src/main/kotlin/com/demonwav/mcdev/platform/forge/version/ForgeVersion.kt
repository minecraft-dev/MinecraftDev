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

import com.demonwav.mcdev.util.getMajorVersion
import com.demonwav.mcdev.util.sortVersions
import java.io.IOException
import java.net.URL
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

class ForgeVersion private constructor(val versions: List<String>) {

    val sortedMcVersions: List<String> by lazy {
        val unsortedVersions = versions.asSequence()
            .mapNotNull(fun (version: String): String? {
                val index = version.indexOf('-')
                if (index == -1) {
                    return null
                }
                return version.substring(0, index)
            }).distinct()
            .toList()
        return@lazy sortVersions(unsortedVersions)
    }

    fun getForgeVersions(mcVersion: String): ArrayList<String> {
        return versions.filterTo(ArrayList()) { it.startsWith(getMajorVersion(mcVersion)) }
    }

    companion object {
        fun downloadData(): ForgeVersion? {
            try {
                val url = URL("https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml")
                val result = mutableListOf<String>()
                url.openStream().use { stream ->
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
