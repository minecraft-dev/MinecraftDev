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

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Pinned Templates Tests")
class PinnedTemplatesTest {

    @TempDir
    lateinit var dir: Path

    private fun descriptor(path: String, json: String) {
        val file = dir.resolve(path)
        file.parent.createDirectories()
        file.writeText(json)
    }

    @Test
    @DisplayName("The templates submodule and its .gitmodules branch are on the format version the tests expect")
    fun checkoutFormatVersion() {
        PinnedTemplates.requireExpectedFormat()
    }

    @Test
    @DisplayName("A checkout on another format version names each template that doesn't match")
    fun wrongVersion() {
        descriptor("forge/.mcdev.template.json", """{"version": 3}""")
        descriptor("sponge/.mcdev.template.json", "not json")
        descriptor("bukkit/paper.mcdev.template.json", """{"version": 4}""")

        val problem = PinnedTemplates.checkFormatVersion(dir, expected = 4) ?: fail("The mismatch wasn't caught")
        assertTrue("forge" in problem, problem)
        assertTrue("sponge" in problem, problem)
        assertFalse("bukkit/paper" in problem, problem)
    }
}
