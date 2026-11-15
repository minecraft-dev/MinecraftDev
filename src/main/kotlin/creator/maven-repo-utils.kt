/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.McdevHttp
import java.io.IOException
import java.util.function.Predicate
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

@Throws(IOException::class)
suspend fun collectMavenVersions(
    url: String,
    headers: Map<String, String> = emptyMap(),
    filter: Predicate<String> = Predicate { true }
): List<String> = McdevHttp.get(url, headers).suspend().connect {
    val result = mutableListOf<String>()
    inputStream.use { stream ->
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
            if (filter.test(version)) {
                result += version
            }
        }
    }

    result
}

@Throws(IOException::class)
suspend fun scrapeArtifactoryDirectoryListing(url: String): List<String> = McdevHttp.get(url).suspend().connect {
    val result = mutableListOf<String>()
    inputStream.use { stream ->
        val inputFactory = XMLInputFactory.newInstance()

        @Suppress("UNCHECKED_CAST")
        val reader = inputFactory.createXMLEventReader(stream) as Iterator<XMLEvent>
        for (event in reader) {
            if (!event.isStartElement) {
                continue
            }
            val start = event.asStartElement()
            val name = start.name.localPart
            if (name != "a") {
                continue
            }

            val childPathEvent = reader.next()
            if (!childPathEvent.isCharacters) {
                continue
            }

            val childPath = childPathEvent.asCharacters().data
            if (childPath != "../") {
                result += childPath
            }
        }
    }

    result
}
