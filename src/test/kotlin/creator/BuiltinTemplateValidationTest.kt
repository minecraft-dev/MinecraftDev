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

import com.demonwav.mcdev.creator.custom.TemplateValidationReporterImpl
import com.demonwav.mcdev.creator.custom.providers.InvalidVersionTemplate
import com.demonwav.mcdev.framework.LogCapture
import com.demonwav.mcdev.framework.http.HttpMockExtension
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@DisplayName("Built-in Template Validation Tests")
class BuiltinTemplateValidationTest : CreatorTemplateProcessorTestBase() {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    private val templateNames = listOf(
        "architectury",
        "bukkit/paper",
        "bukkit/spigot",
        "bungeecord",
        "fabric",
        "forge",
        "multiloader",
        "neoforge",
        "sponge",
        "velocity",
    )

    private fun test(block: suspend () -> Unit) = runBlocking { withTimeout(60.seconds) { block() } }

    private fun validate(name: String) = test {
        LogCapture().use { log ->
            val template = PinnedTemplates.load(wizardContext, name)
            assertTrue(template.isValid, "$name isn't valid")

            val reporter = TemplateValidationReporterImpl()
            processor.setupTemplate(template, reporter)
            assertFalse(reporter.hasErrors, "Template errors: ${reporter.items}")
            assertFalse(reporter.hasWarns, "Template warnings: ${reporter.items}")

            // Failed downloads only log a warning, so check those too
            session.awaitIdle()
            log.assertNoErrors()
            assertEquals(emptyList<String>(), log.warnings, "Warnings were logged")
        }
    }

    @Test
    @DisplayName("The local template provider finds every built-in template")
    fun loadAll() = test {
        // A missing or mismatched checkout should fail with its own message, not as a list of names that differ
        PinnedTemplates.requireExpectedFormat()
        assertEquals(templateNames, PinnedTemplates.names(), "Each built-in template needs a validation test")

        LogCapture().use { log ->
            val templates = PinnedTemplates.loadAll(wizardContext)
            assertEquals(templateNames.size, templates.size, "Loaded templates: ${templates.map { it.label }}")
            for (template in templates) {
                assertFalse(template is InvalidVersionTemplate, "${template.label} has an unsupported format version")
                assertTrue(template.isValid, "${template.label} isn't valid")
            }
            log.assertNoErrors()
        }
    }

    @Test
    @DisplayName("Architectury")
    fun architectury() = validate("architectury")

    @Test
    @DisplayName("Paper")
    fun paper() = validate("bukkit/paper")

    @Test
    @DisplayName("Spigot")
    fun spigot() = validate("bukkit/spigot")

    @Test
    @DisplayName("BungeeCord")
    fun bungeeCord() = validate("bungeecord")

    @Test
    @DisplayName("Fabric")
    fun fabric() = validate("fabric")

    @Test
    @DisplayName("Forge")
    fun forge() = validate("forge")

    @Test
    @DisplayName("Multiloader")
    fun multiloader() = validate("multiloader")

    @Test
    @DisplayName("NeoForge")
    fun neoForge() = validate("neoforge")

    @Test
    @DisplayName("Sponge")
    fun sponge() = validate("sponge")

    @Test
    @DisplayName("Velocity")
    fun velocity() = validate("velocity")
}
