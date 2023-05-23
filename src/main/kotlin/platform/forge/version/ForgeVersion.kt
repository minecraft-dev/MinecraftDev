/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.forge.version

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.sortVersions
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.intellij.openapi.diagnostic.logger
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
                },
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
        private val LOGGER = logger<ForgeVersion>()

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
                LOGGER.error("Failed to retrieve Forge version data", e)
            }
            return null
        }
    }
}
