/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.version

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import java.io.IOException
import java.net.URL
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

class ArchitecturyVersion private constructor(val versions: List<String>) {

    fun getArchitecturyVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        return versions.asSequence()
            .filter {
                it.startsWith(
                    when {
                        mcVersion < MinecraftVersions.MC1_17 -> "1"
                        mcVersion >= MinecraftVersions.MC1_17 && mcVersion < MinecraftVersions.MC1_18 -> "2"
                        mcVersion >= MinecraftVersions.MC1_18 -> "3"
                        else -> "0"
                    }
                )
            }
            .mapNotNull {
                try {
                    SemanticVersion.parse(it)
                } catch (ignore: Exception) {
                    null
                }
            }
            .sortedDescending()
            .take(50)
            .toList()
    }

    companion object {
        fun downloadData(): ArchitecturyVersion? {
            try {
                val url1 = URL("https://maven.architectury.dev/dev/architectury/architectury/maven-metadata.xml")
                val url2 = URL("https://maven.architectury.dev/me/shedaniel/architectury/maven-metadata.xml")
                val result = mutableListOf<String>()
                url1.openStream().use { stream ->
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
                        // val index = version.indexOf('-')
                        // if (index == -1) {
                        //     continue
                        // }

                        result += version
                    }
                }
                url2.openStream().use { stream ->
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
                        // val index = version.indexOf('-')
                        // if (index == -1) {
                        //     continue
                        // }

                        result += version
                    }
                }

                return ArchitecturyVersion(result)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    }
}
