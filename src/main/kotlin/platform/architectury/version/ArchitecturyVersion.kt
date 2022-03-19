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

import com.demonwav.mcdev.util.SemanticVersion
import com.jetbrains.rd.util.first
import java.io.IOException
import java.net.URL
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ArchitecturyVersion private constructor(val versions: Map<SemanticVersion, List<SemanticVersion>>) {

    fun getArchitecturyVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        return versions[mcVersion]!!.asSequence()
            .sortedDescending()
            .take(50)
            .toList()
    }

    companion object {
        private fun findMcVersion(architecturyVersion: SemanticVersion): SemanticVersion {
            val meta = Json.parseToJsonElement(
                URL(
                    "https://gist.githubusercontent.com" +
                        "/shedaniel/4a37f350a6e49545347cb798dbfa72b3" +
                        "/raw/architectury.json"
                ).readText()
            ).jsonObject
            return SemanticVersion.parse(
                meta["versions"]!!.jsonObject.filter {
                    it.value.jsonObject["api"]!!.jsonObject["filter"]!!.jsonPrimitive.content.toRegex().matches(
                        architecturyVersion.toString()
                    )
                }.first().key
            )
        }
        fun downloadData(): ArchitecturyVersion? {
            try {
                val url1 = URL("https://maven.architectury.dev/dev/architectury/architectury/maven-metadata.xml")
                val url2 = URL("https://maven.architectury.dev/me/shedaniel/architectury/maven-metadata.xml")
                val result = mutableMapOf<SemanticVersion, MutableList<SemanticVersion>>()
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
                        if (result.containsKey(findMcVersion(SemanticVersion.parse(version)))) {
                            result[findMcVersion(SemanticVersion.parse(version))]!!.add(SemanticVersion.parse(version))
                        } else {
                            result[findMcVersion(SemanticVersion.parse(version))] = mutableListOf(
                                SemanticVersion.parse(version)
                            )
                        }
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

                        if (result.containsKey(findMcVersion(SemanticVersion.parse(version)))) {
                            result[findMcVersion(SemanticVersion.parse(version))]!!.add(SemanticVersion.parse(version))
                        } else {
                            result[findMcVersion(SemanticVersion.parse(version))] = mutableListOf(
                                SemanticVersion.parse(version)
                            )
                        }
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
