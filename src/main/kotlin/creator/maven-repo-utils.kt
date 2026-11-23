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
import com.demonwav.mcdev.util.fetchCatching
import com.demonwav.mcdev.util.fetchOrNull
import com.demonwav.mcdev.util.loggerForTopLevel
import java.io.IOException
import java.io.InputStream
import java.util.function.Predicate
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException

private val LOGGER = loggerForTopLevel()

/**
 * Returns the versions in the Maven metadata at [url] that match [filter], or a failure after logging a warning if
 * the download or parse fails.
 */
suspend fun collectMavenVersions(
    url: String,
    headers: Map<String, String> = emptyMap(),
    filter: Predicate<String> = Predicate { true }
): Result<List<String>> = fetchCatching(LOGGER, url) { fetchMavenVersions(url, headers, filter) }

/**
 * Like [collectMavenVersions], but throws on failure.
 */
@Throws(IOException::class, XMLStreamException::class)
internal suspend fun fetchMavenVersions(
    url: String,
    headers: Map<String, String> = emptyMap(),
    filter: Predicate<String> = Predicate { true }
): List<String> = McdevHttp.get(url, headers).suspend().connect {
    inputStream.use { stream -> readElementTexts(stream, "version").filter(filter::test) }
}

/**
 * Returns the entries of the directory listing at [url], or null after logging a warning if the download or parse
 * fails. Directories end with `/`.
 */
suspend fun scrapeArtifactoryDirectoryListing(url: String): List<String>? =
    fetchOrNull(LOGGER, url) { fetchArtifactoryDirectoryListing(url) }

/**
 * Like [scrapeArtifactoryDirectoryListing], but throws on failure.
 */
@Throws(IOException::class, XMLStreamException::class)
internal suspend fun fetchArtifactoryDirectoryListing(url: String): List<String> = McdevHttp.get(url).suspend().connect {
    inputStream.use { stream -> readElementTexts(stream, "a").filter { it != "../" } }
}

private fun readElementTexts(stream: InputStream, element: String): List<String> {
    val factory = XMLInputFactory.newInstance()
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    val reader = factory.createXMLStreamReader(stream)
    try {
        val result = mutableListOf<String>()
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.localName == element) {
                val text = reader.elementText.trim()
                if (text.isNotEmpty()) {
                    result += text
                }
            }
        }
        return result
    } finally {
        reader.close()
    }
}
