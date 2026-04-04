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

package com.demonwav.mcdev.creator.custom.derivation

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FetchPaperDependencyVersionForMcVersion : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any {
        val version = parentValues[0] as SemanticVersion
        if (version < MinecraftVersions.MC_26_1) {
            return "${version}-R0.1-SNAPSHOT";
        }

        val isMaven = (parentValues[1] as String) == "Maven"
        if (isMaven) {
            val latestBuild = fetchPaperBuildData(version.toString());
            return "${version}.build.${latestBuild.id}-${latestBuild.channel}"
        }
        return "${version}.build.+"
    }

    private fun fetchPaperBuildData(version: String): PaperBuild {
        return runBlocking {
            val client = HttpClient()
            val response = client.get("https://fill.papermc.io/v3/projects/paper/versions/${version}/builds", block = {
                this.header(
                    "User-Agent",
                    "minecraft-dev/${PluginUtil.pluginVersion} (https://github.com/minecraft-dev/MinecraftDev)"
                )
            })
            if (response.status.isSuccess()) {
                return@runBlocking Json.parseToJsonElement(response.bodyAsText()).jsonArray[0]
                    .let { build ->
                        return@let PaperBuild(
                            build.jsonObject["id"]!!.jsonPrimitive.int,
                            build.jsonObject["channel"]!!.jsonPrimitive.content.lowercase()
                        )
                    }
            } else {
                throw IllegalStateException("Failed to fetch latest Paper build for version ${version}: ${response.bodyAsText()}")
            }
        }
    }

    private data class PaperBuild(
        val id: Int,
        val channel: String,
    );

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (parents.isNullOrEmpty()) {
                reporter.error("Expected a parent")
                return null
            }

            if (!parents[0]!!.acceptsType(SemanticVersion::class.java)) {
                reporter.error("First parent must produce a semantic version")
                return null
            }

            if (!parents[1]!!.acceptsType(String::class.java)) {
                reporter.error("Second parent must produce a string")
                return null
            }

            return FetchPaperDependencyVersionForMcVersion()
        }
    }
}
