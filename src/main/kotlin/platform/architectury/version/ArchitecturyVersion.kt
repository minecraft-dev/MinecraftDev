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
import java.io.IOException
import java.net.URL
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ArchitecturyVersion private constructor(
    val versions: Map<SemanticVersion, List<SemanticVersion>>,
    private val mcVersions: MutableList<List<SemanticVersion>>
) {

    fun getArchitecturyVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        val roundedVersion = mcVersions.find { mcVersion >= it[0] && mcVersion < it[1] }?.first()
        return try {
            versions[roundedVersion]?.asSequence()
                ?.sortedDescending()
                ?.take(50)
                ?.toList() ?: throw IOException("Could not find any architectury versions for $mcVersion")
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    companion object {
        private val updateUrl = URL(
            "https://gist.githubusercontent.com" +
                "/shedaniel/4a37f350a6e49545347cb798dbfa72b3" +
                "/raw/architectury.json"
        )

        fun downloadData(): ArchitecturyVersion? {
            try {
                val meta = Json.parseToJsonElement(updateUrl.readText())
                val versions = mutableMapOf<SemanticVersion, MutableList<SemanticVersion>>()
                val mcVersions = meta.jsonObject["versions"]?.jsonObject?.map { SemanticVersion.parse(it.key) }
                    ?.windowed(2, 1)?.toMutableList().also {
                        it?.add(
                            listOf(
                                it.last().last(),
                                SemanticVersion.parse(
                                    when (val part = it.last().last().parts.getOrNull(1)) {
                                        is SemanticVersion.Companion.VersionPart.ReleasePart ->
                                            (part.version + 1).toString()
                                        null -> "?"
                                        else -> part.versionString
                                    }
                                )
                            )
                        )
                    } ?: throw IOException("Could not find any minecraft versions")

                meta.jsonObject["versions"]?.jsonObject?.forEach {
                    val mcVersion = SemanticVersion.parse(it.key)
                    URL(
                        it.value.jsonObject["api"]?.jsonObject?.get("pom")?.jsonPrimitive?.content ?: throw IOException(
                            "Could not find pom for $mcVersion"
                        )
                    )
                        .openStream().use { stream ->
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
                                val regex = it.value.jsonObject["api"]?.jsonObject?.get("filter")
                                    ?.jsonPrimitive?.content?.toRegex()
                                    ?: throw IOException("Could not find filter for $mcVersion")
                                if (regex.matches(version)) {
                                    versions.getOrPut(mcVersion) { mutableListOf() }.add(SemanticVersion.parse(version))
                                }
                            }
                        }
                }

                return ArchitecturyVersion(versions, mcVersions)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
    }
}
