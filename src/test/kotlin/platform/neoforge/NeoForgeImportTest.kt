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

package com.demonwav.mcdev.platform.neoforge

import com.demonwav.mcdev.framework.BaseGradleImportTest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.neoforge.version.platform.neoforge.version.NeoModDevVersion
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest

@DisplayName("NeoForge Import Tests")
class NeoForgeImportTest : BaseGradleImportTest() {

    private fun prepareTest(neoModDevVersion: String, neoForgeVersion: String) {
        createProjectSubFile(
            GRADLE_WRAPPER_PROPERTIES,
            "distributionUrl=https://services.gradle.org/distributions/gradle-8.8-bin.zip"
        )

        createProjectSubFile(
            BUILD_GRADLE,
            """
                plugins {
                    id 'net.neoforged.moddev' version '$neoModDevVersion'
                }
    
                neoForge {
                    version = "$neoForgeVersion"
                }
            """.trimIndent()
        )

        createProjectSubFile(
            SETTINGS_GRADLE,
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        maven { url = 'https://maven.neoforged.net/releases' }
                    }
                }
            """
        )

        importProject(true)
        importProject(true) // Import twice otherwise McpModule settings won't be populated properly
    }

    @CartesianTest
    @CartesianTest.MethodFactory("versionFactory")
    @DisplayName("NeoForge Imported Data")
    fun neoForgeImportedData(
        neoModDevVersion: String,
        neoForgeAndExpectedMcVersion: Pair<String, String>
    ) {
        val (neoForgeVersion, expectedMcVersion) = neoForgeAndExpectedMcVersion
        prepareTest(neoModDevVersion, neoForgeVersion)

        val module = findMainModule()
        module.assertDetectedPlatformTypes(PlatformType.NEOFORGE, PlatformType.MCP, PlatformType.MIXIN)

        val mcpModule = module.assertMinecraftModule(McpModuleType)
        val mcpSettings = mcpModule.getSettings()
        assertEquals(mcpSettings.platformVersion, neoForgeVersion)
        assertEquals(mcpSettings.minecraftVersion, expectedMcVersion)
    }

    companion object {
        @JvmStatic
        @Suppress("unused") // Used in @CartesianTest.MethodFactory
        fun versionFactory(): ArgumentSets {
            val neoModDevVersions = mutableListOf("2.0.7-beta", "1.0.17", "0.1.131")

            val latestNeoModDev = runBlocking { NeoModDevVersion.downloadData()?.versions?.firstOrNull() }
            if (latestNeoModDev != null) {
                neoModDevVersions.add(0, latestNeoModDev.toString())
            }

            return ArgumentSets
                .argumentsForFirstParameter(neoModDevVersions)
                .argumentsForNextParameter(
                    "21.1.8" to "1.21.1",
                    "21.0.167" to "1.21",
                )
        }
    }
}
