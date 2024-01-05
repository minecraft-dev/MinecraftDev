/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.neoforge.version

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.intellij.openapi.diagnostic.logger
import java.io.IOException
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

class NeoForgeVersion private constructor(val versions: List<String>) {

    val sortedMcVersions: List<SemanticVersion> by lazy {
        val version = versions.asSequence()
            .map { it.substringBeforeLast('.') }
            .distinct()
            .mapNotNull {
                val shortVersion = SemanticVersion.tryParse(it)
                if (shortVersion != null) {
                    val parts = shortVersion.parts.toMutableList()
                    // Insert the '1.' part to the base neoforge version
                    parts.add(0, SemanticVersion.Companion.VersionPart.ReleasePart(1, "1"))
                    SemanticVersion(parts)
                } else null
            }.distinct()
            .sortedDescending()
            .toList()
        return@lazy version
    }

    fun getForgeVersions(mcVersion: SemanticVersion): List<SemanticVersion> {
        val versionText = mcVersion.toString()
        // Drop the 1. part of the mc version
        val shortMcVersion = versionText.substringAfter('.')
        val toList = versions.asSequence()
            .filter { it.substringBeforeLast('.') == shortMcVersion }
            .mapNotNull(SemanticVersion::tryParse)
            .sortedDescending()
            .take(50)
            .toList()
        return toList
    }

    companion object {
        private val LOGGER = logger<NeoForgeVersion>()

        suspend fun downloadData(): NeoForgeVersion? {
            try {
                val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
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

                        result += versionEvent.asCharacters().data
                    }
                }

                return NeoForgeVersion(result)
            } catch (e: IOException) {
                LOGGER.error("Failed to retrieve NeoForge version data", e)
            }
            return null
        }
    }
}
