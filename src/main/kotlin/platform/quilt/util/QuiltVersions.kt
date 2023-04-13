/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt.util

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

private val LOGGER = logger<QuiltVersions>()

class QuiltVersions(val game: List<Game>, val mappings: List<Mappings>, val loader: List<SemanticVersion>) {
    class Game(val version: String, val stable: Boolean)
    class Mappings(val gameVersion: String, val version: QuiltMappingsVersion)

    class QuiltMappingsVersion(val name: String, val build: Int) : Comparable<QuiltMappingsVersion> {
        override fun toString() = name
        override fun compareTo(other: QuiltMappingsVersion) = build.compareTo(other.build)
    }

    companion object {
        suspend fun downloadData(): QuiltVersions? {
            try {
                val url = "https://meta.quiltmc.org/v3/versions"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val response = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .await()

                val gameList = mutableListOf<Game>()
                // val mappingsList = mutableListOf<Mappings>() TODO: workaround for https://github.com/QuiltMC/update-quilt-meta/issues/9, change back when fixed
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

                    /*val mappings = json["mappings"]?.asJsonArray ?: return null TODO: see above
                    for (mapping in mappings) {
                        val mappingObj = mapping?.asJsonObject ?: return null
                        val gameVersion = mappingObj["gameVersion"]?.asString ?: return null
                        val version = mappingObj["version"]?.asString ?: return null
                        val build = mappingObj["build"]?.asInt ?: return null
                        mappingsList += Mappings(gameVersion, QuiltMappingsVersion(version, build))
                    }*/

                    val loaders = json["loader"]?.asJsonArray ?: return null
                    for (loader in loaders) {
                        val loaderObj = loader?.asJsonObject ?: return null
                        val version = loaderObj["version"]
                            ?.asString
                            ?.let(SemanticVersion.Companion::tryParse)
                            ?: return null
                        loaderList += version
                    }
                }
                // TODO: see above
                val mappingsList = mutableListOf<Mappings>()
                val response2 = manager.get("https://meta.quiltmc.org/v3/versions/quilt-mappings")
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .await()
                response2.body().toStream().use {
                    val json = JsonParser.parseReader(it.reader())?.asJsonArray ?: return null
                    for (mapping in json) {
                        val mappingObj = mapping?.asJsonObject ?: return null
                        val gameVersion = mappingObj["gameVersion"]?.asString ?: return null
                        val version = mappingObj["version"]?.asString ?: return null
                        val build = mappingObj["build"]?.asInt ?: return null
                        mappingsList += Mappings(gameVersion, QuiltMappingsVersion(version, build))
                    }
                }
                return QuiltVersions(gameList, mappingsList, loaderList)
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
