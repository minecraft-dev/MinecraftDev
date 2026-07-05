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

package com.demonwav.mcdev.platform.fabric.creator

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FabricGradleWrapperTest {

    @Test
    fun `writes complete Gradle wrapper to project root`() {
        val projectRoot = Files.createTempDirectory("mcdev-fabric-wrapper")

        FabricGradleWrapper.writeTo(projectRoot)

        assertGradleWrapper(projectRoot)
    }

    @Test
    fun `writes Gradle wrapper when custom template generated a Fabric project`() {
        val projectRoot = Files.createTempDirectory("mcdev-fabric-template-wrapper")
        projectRoot.resolve("build.gradle").writeText(
            """
            plugins {
                id 'fabric-loom' version '${FabricGradleWrapper.LOOM_VERSION}'
            }
            """.trimIndent(),
        )

        assertTrue(FabricGradleWrapper.writeIfFabricProject(projectRoot))
        assertGradleWrapper(projectRoot)
    }

    private fun assertGradleWrapper(projectRoot: java.nio.file.Path) {
        assertTrue(projectRoot.resolve("gradlew").exists())
        assertTrue(projectRoot.resolve("gradlew.bat").exists())
        assertTrue(projectRoot.resolve("gradle/wrapper/gradle-wrapper.jar").exists())

        val wrapperProperties = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties")
        assertTrue(wrapperProperties.exists())

        val wrapperPropertiesText = wrapperProperties.readText()
        assertTrue(wrapperPropertiesText.contains(FabricGradleWrapper.MARKER))
        assertTrue(
            wrapperPropertiesText.contains(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.5.0-bin.zip",
            ),
        )
    }
}
