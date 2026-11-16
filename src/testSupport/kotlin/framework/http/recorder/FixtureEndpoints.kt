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

import com.demonwav.mcdev.creator.custom.npm.NpmRegistry
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A URL to record.
 *
 * @property allowedStatuses statuses to save. Any other status fails the recording. Only a 200 keeps its body.
 * @property expand returns more endpoints to record from this one's untrimmed body. Only called for a 200.
 */
class Endpoint(
    val url: String,
    val trim: Trim = Trim.NONE,
    val allowedStatuses: Set<Int> = setOf(200),
    val expand: ((ByteArray) -> List<Endpoint>)? = null,
)

/**
 * Every URL the creator requests. URLs the plugin hardcodes must be added here. URLs from templates are found
 * automatically.
 *
 * The builtin template zip isn't recorded, the build zips the pinned `templates` checkout instead.
 */
object FixtureEndpoints {
    private const val PARCHMENT_DATA = "https://maven.parchmentmc.org/org/parchmentmc/data/"

    fun all(templatesDir: Path?): List<Endpoint> {
        val endpoints = fixed() + (templatesDir?.let(::fromTemplates) ?: emptyList())
        return endpoints.distinctBy { it.url }
    }

    private fun fixed(): List<Endpoint> = listOf(
        // Fabric
        Endpoint("https://meta.fabricmc.net/v2/versions", FabricMetaTrim()),
        Endpoint("https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml", MavenMetadataTrim()),
        Endpoint("https://api.modrinth.com/v2/project/P7dR8mSH/version", ModrinthVersionsTrim()),

        // Architectury
        Endpoint("https://api.modrinth.com/v2/project/architectury-api/version", ModrinthVersionsTrim()),
        Endpoint(
            "https://maven.architectury.dev/dev/architectury/architectury-loom/maven-metadata.xml",
            MavenMetadataTrim(),
        ),

        // Forge, grouped by Minecraft version, which is everything before the first '-'
        Endpoint(
            "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml",
            MavenMetadataTrim { it.substringBefore('-') },
        ),

        // NeoForge, where the first two parts are the Minecraft version
        Endpoint("https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml", MavenMetadataTrim()),
        Endpoint(
            "https://maven.neoforged.net/releases/net/neoforged/gradle/userdev/maven-metadata.xml",
            MavenMetadataTrim(),
        ),
        Endpoint(
            "https://maven.neoforged.net/releases/net/neoforged/moddev" +
                "/net.neoforged.moddev.gradle.plugin/maven-metadata.xml",
            MavenMetadataTrim(),
        ),

        // Parchment lists its Minecraft versions, then has one metadata file per version. Versions without a release
        // answer 404, which the creator expects, so that's recorded too.
        Endpoint(PARCHMENT_DATA, expand = ::parchmentVersions),

        // Paper
        Endpoint("https://fill.papermc.io/v3/projects/paper", Trim.PRETTY_JSON),
    )

    private fun parchmentVersions(listing: ByteArray): List<Endpoint> {
        val links = Regex("""href="(parchment-[^"/]+)/"""").findAll(listing.toString(Charsets.UTF_8))
        return links.map { it.groupValues[1] }.distinct().map { dir ->
            Endpoint("$PARCHMENT_DATA$dir/maven-metadata.xml", MavenMetadataTrim(), allowedStatuses = setOf(200, 404))
        }.toList()
    }

    /**
     * The `sourceUrl` of every property and the registry entry of every `run_npm_package` finalizer in the templates.
     * `sourceUrl`s containing `$` are skipped.
     */
    fun fromTemplates(templatesDir: Path): List<Endpoint> {
        val templateFiles = Files.walk(templatesDir).use { paths ->
            paths.filter { it.isRegularFile() && it.name.endsWith(".mcdev.template.json") }.sorted().toList()
        }

        val endpoints = mutableListOf<Endpoint>()
        for (file in templateFiles) {
            walk(JsonParser.parseString(file.readText())) { obj ->
                val sourceUrl = obj.stringOrNull("sourceUrl")
                if (sourceUrl != null) {
                    if ('$' in sourceUrl) {
                        System.err.println("Skipping templated sourceUrl in $file: $sourceUrl")
                    } else {
                        endpoints += Endpoint(sourceUrl, MavenMetadataTrim())
                    }
                }

                val npmPackage = obj.stringOrNull("package")
                if (obj.stringOrNull("type") == "run_npm_package" && npmPackage != null) {
                    val version = obj.stringOrNull("version") ?: "latest"
                    endpoints += Endpoint(NpmRegistry.packageUrl(npmPackage, version), Trim.PRETTY_JSON)
                }
            }
        }
        return endpoints
    }

    private fun walk(element: JsonElement, visit: (JsonObject) -> Unit) {
        when (element) {
            is JsonObject -> {
                visit(element)
                element.entrySet().forEach { walk(it.value, visit) }
            }
            is JsonArray -> element.forEach { walk(it, visit) }
            else -> Unit
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val value = get(name)
        return if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) value.asString else null
    }
}
