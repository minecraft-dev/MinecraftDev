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

package com.demonwav.mcdev.framework.http.recorder

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text

/**
 * Rewrites a recorded body before it's saved, to keep fixtures small and their diffs readable. A trim may drop entries
 * or reformat, but the result must still parse as the upstream format.
 */
fun interface Trim {
    fun apply(body: ByteArray): ByteArray

    companion object {
        val NONE = Trim { it }

        /**
         * Pretty prints JSON with sorted keys.
         */
        val PRETTY_JSON = Trim { body -> writeJson(parseJson(body)) }
    }
}

private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

private fun parseJson(body: ByteArray): JsonElement = JsonParser.parseString(body.toString(Charsets.UTF_8))

/**
 * Writes [element] pretty printed with object keys sorted. Some APIs, Modrinth's `hashes` for one, don't keep a stable
 * key order between requests.
 */
private fun writeJson(element: JsonElement): ByteArray =
    (gson.toJson(sortKeys(element)) + "\n").toByteArray(Charsets.UTF_8)

private fun sortKeys(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject().also { sorted ->
        element.keySet().sorted().forEach { key -> sorted.add(key, sortKeys(element.get(key))) }
    }
    is JsonArray -> element.map(::sortKeys).toJsonArray()
    else -> element
}

private val releaseVersion = Regex("""\d+(\.\d+)+""")

/**
 * The first two numeric parts of a version, ignoring any `-` or `+` suffix: `1.20.1-47.2.0` becomes `1.20`.
 */
fun majorMinor(version: String): String =
    version.substringBefore('-').substringBefore('+').split('.').take(2).joinToString(".")

/**
 * Keeps the newest [keepPerGroup] versions per [groupKey] in a `maven-metadata.xml`'s `<versions>` list. Metadata
 * without that list is returned unchanged.
 */
class MavenMetadataTrim(
    private val keepPerGroup: Int = 3,
    private val groupKey: (String) -> String = ::majorMinor,
) : Trim {
    override fun apply(body: ByteArray): ByteArray {
        val document = documentBuilder().parse(ByteArrayInputStream(body))
        val versions = document.documentElement.child("versioning")?.child("versions") ?: return body

        val elements = versions.children("version")
        val keep = elements.map { it.textContent.trim() }
            .groupBy(groupKey).values
            .flatMapTo(HashSet()) { it.sortedWith(NaturalOrder.reversed()).take(keepPerGroup) }

        for (element in elements) {
            if (element.textContent.trim() !in keep) {
                // Remove the indentation of a version on its own line too, or it leaves a blank line
                val before = element.previousSibling
                val after = element.nextSibling
                if (before.isBlankText() && (after == null || after.isBlankText())) {
                    versions.removeChild(before)
                }
                versions.removeChild(element)
            }
        }

        // The JDK's serializer puts no newline after the declaration or at the end, so write those by hand
        val out = ByteArrayOutputStream()
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".toByteArray())
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }.transform(DOMSource(document), StreamResult(out))
        out.write('\n'.code)
        return out.toByteArray()
    }

    @Suppress("HttpUrlsUsage")
    private fun documentBuilder() = DocumentBuilderFactory.newInstance().apply {
        // The input comes from the network
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder()
}

/**
 * Trims `meta.fabricmc.net/v2/versions`. Keeps every stable game version plus the newest [unstableGames] unstable ones,
 * and for each kept version the newest [mappingsPerGame] Yarn builds and its intermediary. Also keeps the newest
 * [loaders] and [installers].
 */
class FabricMetaTrim(
    private val unstableGames: Int = 10,
    private val mappingsPerGame: Int = 2,
    private val loaders: Int = 10,
    private val installers: Int = 3,
) : Trim {
    override fun apply(body: ByteArray): ByteArray {
        val json = parseJson(body).asJsonObject

        val game = json.getAsJsonArray("game")
        val unstableKept = game.map { it.asJsonObject }.filter { !it.bool("stable") }.take(unstableGames).toSet()
        val keptGame = game.filter { it.asJsonObject.bool("stable") || it.asJsonObject in unstableKept }
        json.add("game", keptGame.toJsonArray())
        val gameVersions = keptGame.mapTo(HashSet()) { it.asJsonObject.string("version") }

        json.getAsJsonArray("mappings")?.let { mappings ->
            val byGame = mappings.map { it.asJsonObject }.filter { it.string("gameVersion") in gameVersions }
            val keep = byGame.groupBy { it.string("gameVersion") }.values
                .flatMapTo(HashSet()) { group ->
                    group.sortedByDescending { it.get("build").asInt }.take(mappingsPerGame)
                }
            json.add("mappings", byGame.filter { it in keep }.toJsonArray())
        }
        json.getAsJsonArray("intermediary")?.let { intermediary ->
            val kept = intermediary.filter { it.asJsonObject.string("version") in gameVersions }
            json.add("intermediary", kept.toJsonArray())
        }
        json.getAsJsonArray("loader")?.let { json.add("loader", it.take(loaders).toJsonArray()) }
        json.getAsJsonArray("installer")?.let { json.add("installer", it.take(installers).toJsonArray()) }

        return writeJson(json)
    }
}

/**
 * Trims a Modrinth `/v2/project/<id>/version` list. Keeps the [newest] newest versions, plus the newest version for
 * each pair of release game version and loader. Entries stay in their original order, without the `downloads` count,
 * which changes on every request.
 */
class ModrinthVersionsTrim(private val newest: Int = 5) : Trim {
    override fun apply(body: ByteArray): ByteArray {
        val entries = parseJson(body).asJsonArray.map { it.asJsonObject }
        val byDate = entries.sortedByDescending { it.get("date_published")?.asString.orEmpty() }

        val keep = byDate.take(newest).toMutableSet()
        val seen = HashSet<Pair<String, String>>()
        for (entry in byDate) {
            val loaders = entry.getAsJsonArray("loaders")?.map { it.asString }.orEmpty().ifEmpty { listOf("") }
            for (gameVersion in entry.getAsJsonArray("game_versions")?.map { it.asString }.orEmpty()) {
                if (!releaseVersion.matches(gameVersion)) {
                    continue
                }
                for (loader in loaders) {
                    if (seen.add(gameVersion to loader)) {
                        keep += entry
                    }
                }
            }
        }

        // JsonObject equality is structural, so only edit entries once they're out of the set
        val kept = entries.filter { it in keep }.onEach { it.remove("downloads") }
        return writeJson(kept.toJsonArray())
    }
}

/**
 * Orders versions by comparing runs of digits as numbers and everything else as text. Pre-releases may sort out of
 * place, such as `1.0-rc1` after `1.0`.
 */
object NaturalOrder : Comparator<String> {
    private val token = Regex("""\d+|\D+""")

    override fun compare(a: String, b: String): Int {
        val left = token.findAll(a).map { it.value }.toList()
        val right = token.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(left.size, right.size)) {
            val l = left[i]
            val r = right[i]
            val result = if (l[0].isDigit() && r[0].isDigit()) {
                BigInteger(l).compareTo(BigInteger(r))
            } else {
                l.compareTo(r)
            }
            if (result != 0) {
                return result
            }
        }
        return left.size.compareTo(right.size)
    }
}

private fun JsonObject.bool(name: String): Boolean = get(name)?.asBoolean ?: false

private fun JsonObject.string(name: String): String = get(name)?.asString.orEmpty()

private fun Element.children(name: String): List<Element> =
    (0 until childNodes.length).map(childNodes::item).filterIsInstance<Element>().filter { it.tagName == name }

private fun Element.child(name: String): Element? = children(name).firstOrNull()

private fun Node?.isBlankText(): Boolean = this is Text && data.isBlank()

private fun Iterable<JsonElement>.toJsonArray(): JsonArray = JsonArray().also { array -> forEach(array::add) }
