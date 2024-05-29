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

package com.demonwav.mcdev.platform.fabric.util

import com.demonwav.mcdev.creator.custom.model.TemplateApi
import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

private val LOGGER = logger<FabricVersions>()

class FabricVersions(val game: List<Game>, val mappings: List<Mappings>, val loader: List<SemanticVersion>) {
    class Game(val version: String, val stable: Boolean)
    class Mappings(val gameVersion: String, val version: YarnVersion)

    @TemplateApi
    class YarnVersion(val name: String, val build: Int) : Comparable<YarnVersion> {
        override fun toString() = name
        override fun compareTo(other: YarnVersion) = build.compareTo(other.build)
    }

    companion object {
        suspend fun downloadData(): FabricVersions? {
            try {
                val url = "https://meta.fabricmc.net/v2/versions"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val response = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .await()

                val gameList = mutableListOf<Game>()
                val mappingsList = mutableListOf<Mappings>()
                val loaderList = mutableListOf<SemanticVersion>()
                response.body().toStream().use { stream ->
                    val json = JsonParser.parseReader(stream.reader())?.asJsonObject ?: return null

                    val game = json["game"]?.asJsonArray ?: return null
                    for (version in game) {
                        val versionObj = version?.asJsonObject ?: return null
                        val gameVer = versionObj["version"]?.asString ?: return null
                        val stable = versionObj["stable"]?.asBoolean ?: return null
                        gameList += Game(gameVer, stable)
                    }

                    val mappings = json["mappings"]?.asJsonArray ?: return null
                    for (mapping in mappings) {
                        val mappingObj = mapping?.asJsonObject ?: return null
                        val gameVersion = mappingObj["gameVersion"]?.asString ?: return null
                        val version = mappingObj["version"]?.asString ?: return null
                        val build = mappingObj["build"]?.asInt ?: return null
                        mappingsList += Mappings(gameVersion, YarnVersion(version, build))
                    }

                    val loaders = json["loader"]?.asJsonArray ?: return null
                    for (loader in loaders) {
                        val loaderObj = loader?.asJsonObject ?: return null
                        val version = loaderObj["version"]?.asString?.let(SemanticVersion::tryParse) ?: return null
                        loaderList += version
                    }
                }
                return FabricVersions(gameList, mappingsList, loaderList)
            } catch (e: IOException) {
                LOGGER.error(e)
            } catch (e: JsonSyntaxException) {
                LOGGER.error(e)
            } catch (e: IllegalStateException) {
                LOGGER.error(e)
            } catch (e: UnsupportedOperationException) {
                LOGGER.error(e)
            }
            return null
        }
    }
}
