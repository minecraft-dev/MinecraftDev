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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.adventure.framework.AdventureLibraryDetector
import com.demonwav.mcdev.platform.bukkit.framework.ModernPaperLibraryDetector
import com.demonwav.mcdev.platform.mcp.framework.McpLibraryDetector
import com.demonwav.mcdev.platform.mixin.framework.MixinLibraryDetector
import com.demonwav.mcdev.platform.sponge.framework.SpongeLibraryDetector
import com.demonwav.mcdev.platform.velocity.framework.VelocityLibraryDetector
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScopes
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinecraftLibraryDetectorTest : ProjectBuilderTest() {

    @Test
    fun `maven detector validates path coordinates and version`() {
        val strictDetector = object : MavenMinecraftLibraryDetector(
            PlatformType.BUKKIT,
            "org.bukkit",
            "bukkit",
        ) {}
        val compatibleDetector = object : MavenMinecraftLibraryDetector(
            PlatformType.PAPER,
            "io.papermc.paper",
            "paper-api",
            strict = false,
        ) {}

        val strictMatch = createRoot(
            "strict-match",
            mapOf(
                "META-INF/maven/org.bukkit/bukkit/pom.properties" to
                    "groupId=org.bukkit\nartifactId=bukkit\nversion=1.0\n",
            ),
        )
        val wrongCoordinates = createRoot(
            "wrong-coordinates",
            mapOf(
                "META-INF/maven/org.bukkit/bukkit/pom.properties" to
                    "groupId=example\nartifactId=other\nversion=1.0\n",
            ),
        )
        val missingVersion = createRoot(
            "missing-version",
            mapOf(
                "META-INF/maven/org.bukkit/bukkit/pom.properties" to
                    "groupId=org.bukkit\nartifactId=bukkit\n",
            ),
        )
        val paperCompatible = createRoot(
            "paper-compatible",
            mapOf(
                "META-INF/maven/io.papermc.paper/paper-api/pom.properties" to
                    "groupId=legacy.paper\nartifactId=legacy-api\nversion=1.0\n",
            ),
        )

        assertTrue(detect(strictDetector, strictMatch))
        assertFalse(detect(strictDetector, wrongCoordinates))
        assertFalse(detect(strictDetector, missingVersion))
        assertTrue(detect(compatibleDetector, paperCompatible))
    }

    @Test
    fun `manifest and service detectors preserve their content rules`() {
        val adventure = createRoot(
            "adventure",
            mapOf(
                "META-INF/MANIFEST.MF" to
                    "Manifest-Version: 1.0\nAutomatic-Module-Name: net.kyori.adventure\n\n",
            ),
        )
        val spongeWithoutVersion = createRoot(
            "sponge-without-version",
            mapOf(
                "META-INF/MANIFEST.MF" to
                    "Manifest-Version: 1.0\nImplementation-Title: SpongeAPI\n\n",
            ),
        )
        val mixin = createRoot(
            "mixin",
            mapOf("META-INF/services/org.spongepowered.asm.service.IMixinService" to ""),
        )
        val velocity = createRoot(
            "velocity",
            mapOf(
                "META-INF/services/javax.annotation.processing.Processor" to
                    "com.example.OtherProcessor\ncom.velocitypowered.api.plugin.ap.PluginAnnotationProcessor\n",
            ),
        )
        val wrongVelocity = createRoot(
            "wrong-velocity",
            mapOf(
                "META-INF/services/javax.annotation.processing.Processor" to
                    "com.example.OtherProcessor\n",
            ),
        )

        assertTrue(detect(AdventureLibraryDetector(), adventure))
        assertFalse(detect(SpongeLibraryDetector(), spongeWithoutVersion))
        assertTrue(detect(MixinLibraryDetector(), mixin))
        assertTrue(detect(VelocityLibraryDetector(), velocity))
        assertFalse(detect(VelocityLibraryDetector(), wrongVelocity))
    }

    @Test
    fun `modern paper requires both class and versioning resource`() {
        val both = createRoot(
            "modern-paper",
            mapOf(
                "io/papermc/paper/ServerBuildInfo.java" to
                    "package io.papermc.paper; public interface ServerBuildInfo {}",
                "apiVersioning.json" to "{}",
            ),
        )
        val classOnly = createRoot(
            "modern-paper-class-only",
            mapOf(
                "io/papermc/paper/ServerBuildInfo.java" to
                    "package io.papermc.paper; public interface ServerBuildInfo {}",
            ),
        )
        val resourceOnly = createRoot(
            "modern-paper-resource-only",
            mapOf("apiVersioning.json" to "{}"),
        )

        val detector = ModernPaperLibraryDetector()
        assertTrue(detect(detector, both))
        assertFalse(detect(detector, classOnly))
        assertFalse(detect(detector, resourceOnly))
    }

    @Test
    fun `class detector cannot escape the supplied scope`() {
        val minecraft = createRoot(
            "minecraft",
            mapOf(
                "net/minecraft/server/MinecraftServer.java" to
                    "package net.minecraft.server; public class MinecraftServer {}",
            ),
        )
        val unrelated = createRoot(
            "unrelated",
            mapOf("example/Placeholder.java" to "package example; public class Placeholder {}"),
        )

        val detector = McpLibraryDetector()
        assertTrue(detect(detector, minecraft))
        assertFalse(detect(detector, unrelated))
    }

    private fun createRoot(name: String, files: Map<String, String>): VirtualFile {
        var firstFile: VirtualFile? = null
        buildProject {
            for ((path, content) in files) {
                val extension = path.substring(path.lastIndexOf('.'))
                val file = file("$name/$path", content, extension, configure = false, allowAst = true)
                if (firstFile == null) {
                    firstFile = file
                }
            }
        }

        return generateSequence(firstFile) { it.parent }
            .first { it.name == name }
    }

    private fun detect(detector: MinecraftLibraryDetector, root: VirtualFile): Boolean {
        DumbService.getInstance(project).waitForSmartMode()
        val scope = GlobalSearchScopes.directoryScope(project, root, true)
        return runReadAction { detector.isLibraryPresent(project, scope) }
    }
}
