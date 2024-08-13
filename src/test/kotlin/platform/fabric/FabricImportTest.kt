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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.creator.collectMavenLatestAndReleaseVersion
import com.demonwav.mcdev.framework.BaseGradleImportTest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junitpioneer.jupiter.cartesian.ArgumentSets
import org.junitpioneer.jupiter.cartesian.CartesianTest

@DisplayName("Fabric Import Tests")
class FabricImportTest : BaseGradleImportTest() {

    private fun prepareTest(
        loomVersion: String,
        minecraftVersion: String,
        yarnMappings: String,
        loaderVersion: String,
        splitSources: Boolean,
    ) {
        createProjectSubFile(
            GRADLE_WRAPPER_PROPERTIES,
            "distributionUrl=https://services.gradle.org/distributions/gradle-8.8-bin.zip"
        )

        createProjectSubFile(
            BUILD_GRADLE,
            """
                plugins {
                    id 'fabric-loom' version '$loomVersion'
                }

                loom {
                    ${if (splitSources) "splitEnvironmentSourceSets()" else ""}

                    mods {
                        "test-mod" {
                            sourceSet sourceSets.main
                            ${if (splitSources) "sourceSet sourceSets.client" else ""}
                        }
                    }
                }

                dependencies {
                    minecraft "com.mojang:minecraft:$minecraftVersion"
                    mappings "net.fabricmc:yarn:$yarnMappings:v2"
                    modImplementation "net.fabricmc:fabric-loader:$loaderVersion"
                }
            """.trimIndent()
        )

        createProjectSubFile(
            SETTINGS_GRADLE,
            """
                pluginManagement {
                    repositories {
                        maven {
                            name = 'Fabric'
                            url = 'https://maven.fabricmc.net/'
                        }
                        gradlePluginPortal()
                    }
                }
            """
        )

        importProject(true)
        importProject(true) // Import twice otherwise McpModule settings won't be populated properly
    }

    @CartesianTest
    @CartesianTest.MethodFactory("argumentsFactory")
    fun fabricImportedData(
        loomVersion: String,
        fabricVersions: TestVersions,
        splitSources: Boolean,
    ) {
        prepareTest(loomVersion, fabricVersions.mc, fabricVersions.yarn, fabricVersions.loader, splitSources)

        val module = findMainModule()
        module.assertDetectedPlatformTypes(PlatformType.FABRIC, PlatformType.MCP, PlatformType.MIXIN)

        val mcpModule = module.assertMinecraftModule(McpModuleType)
        val mcpSettings = mcpModule.getSettings()
        assertEquals(mcpSettings.minecraftVersion, fabricVersions.mc)

        val fabricModule = module.assertMinecraftModule(FabricModuleType)
        val loomData = fabricModule.loomData
        assertNotNull(loomData)

        assertNotNull(loomData!!.tinyMappings)
        assertTrue(loomData.tinyMappings!!.exists())
        assertTrue(loomData.tinyMappings!!.isFile())

        val expectedDecompilers = setOf("cfr", "fernFlower", "vineflower")

        if (splitSources) {
            assertTrue(loomData.splitMinecraftJar)

            // SourceSets
            val modSourceSets = loomData.modSourceSets
            assertNotNull(modSourceSets)
            assertEquals(1, modSourceSets!!.size)

            val testModSourceSets = modSourceSets["test-mod"]
            assertNotNull(testModSourceSets)
            assertContainsElements(testModSourceSets!!, "main", "client")

            // Decompilers
            val decompilers = loomData.decompileTasks
            assertNotNull(decompilers)
            assertEquals(2, decompilers.size)

            val commonDecompilers = decompilers["common"]
            assertNotNull(commonDecompilers)
            assertTrue(commonDecompilers!!.all { it.name in expectedDecompilers })

            val clientDecompilers = decompilers["client"]
            assertNotNull(clientDecompilers)
            assertTrue(clientDecompilers!!.all { it.name in expectedDecompilers })
        } else {
            assertFalse(loomData.splitMinecraftJar)

            // SourceSets
            val modSourceSets = loomData.modSourceSets
            assertNotNull(modSourceSets)
            assertEquals(1, modSourceSets!!.size)

            val testModSourceSets = modSourceSets["test-mod"]
            assertNotNull(testModSourceSets)
            assertContainsElements(testModSourceSets!!, "main")

            // Decompilers
            val decompilers = loomData.decompileTasks
            assertNotNull(decompilers)
            assertEquals(1, decompilers.size)

            val singleDecompilers = decompilers["single"]
            assertNotNull(singleDecompilers)
            assertTrue(singleDecompilers!!.all { it.name in expectedDecompilers })
        }
    }

    data class TestVersions(val mc: String, val yarn: String, val loader: String)

    companion object {
        @JvmStatic
        @Suppress("unused") // Used in @CartesianTest.MethodFactory
        fun argumentsFactory(): ArgumentSets {
            val loomVersions = runBlocking {
                collectMavenLatestAndReleaseVersion(
                    "https://maven.fabricmc.net/fabric-loom/fabric-loom.gradle.plugin/maven-metadata.xml"
                )
            }

            return ArgumentSets.argumentsForFirstParameter(
                loomVersions.latest,
                "1.7-SNAPSHOT",
                "1.6-SNAPSHOT",
                "1.5-SNAPSHOT",
                "1.4-SNAPSHOT",
                "1.3-SNAPSHOT",
            ).argumentsForNextParameter(
                TestVersions("1.21.1", "1.21.1+build.3", "0.16.0"),
                TestVersions("1.21", "1.21+build.9", "0.16.0"),
                TestVersions("1.20.6", "1.20.6+build.3", "0.16.0"),
                TestVersions("1.19.4", "1.19.4+build.2", "0.16.0"),
                TestVersions("1.18.2", "1.18.2+build.4", "0.16.0"),
            ).argumentsForNextParameter(true, false)
        }
    }
}
