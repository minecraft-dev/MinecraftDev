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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.custom.npm.NpmRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("npm Registry Tests")
class NpmRegistryTest {

    private val base = "https://registry.npmjs.org"

    @Test
    @DisplayName("Scoped package names escape their slash")
    fun scopedPackage() {
        assertEquals("$base/@fabricmc%2Fcli/latest", NpmRegistry.packageUrl("@fabricmc/cli", "latest"))
        assertEquals("$base/left-pad/1.3.0", NpmRegistry.packageUrl("left-pad", "1.3.0"))
    }

    @Test
    @DisplayName("Versions are encoded like JavaScript's encodeURIComponent")
    fun versionEncoding() {
        val expected = mapOf(
            "1.0.0-beta.1" to "1.0.0-beta.1",
            "~1.2.3" to "~1.2.3",
            "^1.0.0" to "%5E1.0.0",
            ">=1 <2" to "%3E%3D1%20%3C2",
            "!'()*" to "!'()*",
            "1.x || 2.x" to "1.x%20%7C%7C%202.x",
        )
        for ((version, encoded) in expected) {
            assertEquals("$base/a/$encoded", NpmRegistry.packageUrl("a", version), version)
        }
    }
}
