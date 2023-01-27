/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.util

import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.SemanticVersion
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.requests.suspendable
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.logger
import java.io.IOException

private val LOGGER = logger<FabricApiVersions>()

class FabricApiVersions(val versions: List<Version>) {
    class Version(val gameVersions: List<String>, val version: SemanticVersion)

    companion object {
        suspend fun downloadData(): FabricApiVersions? {
            try {
                val url = "https://api.modrinth.com/v2/project/P7dR8mSH/version"
                val manager = FuelManager()
                manager.proxy = selectProxy(url)

                val response = manager.get(url)
                    .header("User-Agent", PluginUtil.useragent)
                    .suspendable()
                    .await()

                val versions = mutableListOf<Version>()
                response.body().toStream().use { stream ->
                    val json = JsonParser.parseReader(stream.reader())?.asJsonArray ?: return null
                    versionLoop@
                    for (ver in json) {
                        val version = ver?.asJsonObject ?: return null
                        val files = version["files"]?.asJsonArray ?: return null
                        for (file in files) {
                            val fileObj = file?.asJsonObject ?: return null
                            val filename = fileObj["filename"]?.asString ?: return null
                            if (!filename.startsWith("fabric-api-")) {
                                continue@versionLoop
                            }
                        }
                        val versionNumber = version["version_number"]?.asString?.let(SemanticVersion::tryParse)
                            ?: return null
                        val gameVersions = version["game_versions"]?.asJsonArray ?: return null
                        val gameVersionsList = mutableListOf<String>()
                        for (gameVer in gameVersions) {
                            val gameVersion = gameVer?.asString ?: return null
                            gameVersionsList += gameVersion
                        }
                        versions += Version(gameVersionsList, versionNumber)
                    }
                }
                return FabricApiVersions(versions)
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
