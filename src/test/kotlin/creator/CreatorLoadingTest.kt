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

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.creator.custom.CreatorCaches
import com.demonwav.mcdev.creator.custom.CreatorTemplateProcessor
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.TemplateService
import com.demonwav.mcdev.creator.custom.TemplateValidationReporterImpl
import com.demonwav.mcdev.creator.custom.model.ForgeVersions
import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.framework.LogCapture
import com.demonwav.mcdev.framework.http.HttpMockExtension
import com.demonwav.mcdev.framework.http.HttpMockServer.Companion.mockUrl
import com.demonwav.mcdev.util.SemanticVersion
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.observable.properties.PropertyGraph
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir

@DisplayName("Creator Loading Tests")
class CreatorLoadingTest : CreatorTemplateProcessorTestBase() {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    @TempDir
    lateinit var templateDir: Path

    private val forgeUrl = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml"
    private val paperUrl = "https://fill.papermc.io/v3/projects/paper"
    private val gremlinUrl = "https://plugins.gradle.org/m2/xyz/jpenilla/gremlin-gradle/maven-metadata.xml"

    private val template = """
        {
          "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
          "properties": [
            {
              "name": "FORGE_VERSIONS",
              "type": "forge_versions"
            },
            {
              "name": "PAPER_VERSION",
              "type": "paper_versions",
              "default": "1.21"
            },
            {
              "name": "GREMLIN_VERSION",
              "type": "maven_artifact_version",
              "parameters": {
                "sourceUrl": "$gremlinUrl"
              }
            }
          ]
        }
    """.trimIndent()

    private fun test(block: suspend () -> Unit) = runBlocking { withTimeout(30.seconds) { block() } }

    private suspend fun loadTemplate(): LoadedTemplate {
        templateDir.resolve("test.mcdev.template.json").writeText(template)
        val repo = MinecraftSettings.TemplateRepo("Test", "local", templateDir.toString())
        return TemplateProvider.get("local")!!.loadTemplates(wizardContext, repo).single()
    }

    private suspend fun setUpAndWait(processor: CreatorTemplateProcessor, template: LoadedTemplate) {
        val reporter = TemplateValidationReporterImpl()
        processor.setupTemplate(template, reporter)
        assertFalse(reporter.hasErrors, "Template errors: ${reporter.items}")
        assertFalse(reporter.hasWarns, "Template warnings: ${reporter.items}")
        processor.context.session.awaitIdle()
    }

    private fun forgeVersions(processor: CreatorTemplateProcessor = this.processor): ForgeVersions =
        checkNotNull(processor.context.property<ForgeVersions>("FORGE_VERSIONS").get())

    private fun stubForge(vararg versions: String) {
        val versionTags = versions.joinToString("") { "<version>$it</version>" }
        val body = "<metadata><versioning><versions>$versionTags</versions></versioning></metadata>"
        http.server.stubFor(get(mockUrl(forgeUrl)).willReturn(ok(body)))
    }

    private fun newProcessor(): CreatorTemplateProcessor {
        val graph = PropertyGraph()
        val context = WizardContext(null, fixture.testRootDisposable)
        context.overrideModalityState(ModalityState.nonModal())
        val session = TemplateService.instance.openSession("Second creator", fixture.testRootDisposable)
        return CreatorTemplateProcessor(graph, context, session, TestExternalTemplatePropertyProvider(graph))
            .apply { initBuiltinProperties() }
    }

    private fun v(version: String) = SemanticVersion.parse(version)

    @Test
    @DisplayName("A local template loads and its version properties finish loading from the fixtures")
    fun loadFromFixtures() = test {
        LogCapture().use { log ->
            val template = loadTemplate()
            setUpAndWait(processor, template)

            assertEquals(ForgeVersions(v("26.3"), v("66.1.8")), forgeVersions())
            assertTrue(session.isIdle)
            for (url in listOf(forgeUrl, paperUrl, gremlinUrl)) {
                assertEquals(1, http.server.requestsTo(url).size, "Requests to $url")
            }
            log.assertNoErrors()
        }
    }

    // This test and the next override the same endpoint differently, and pass in either order
    @Test
    @DisplayName("Forge versions come from an overridden response")
    fun overrideForgeOld() = test {
        stubForge("1.20.1-47.1.0", "1.20.1-47.2.0", "1.19.4-45.1.0")
        setUpAndWait(processor, loadTemplate())
        assertEquals(ForgeVersions(v("1.20.1"), v("47.2.0")), forgeVersions())
    }

    @Test
    @DisplayName("Forge versions come from a differently overridden response")
    fun overrideForgeNew() = test {
        stubForge("26.1-62.0.5", "1.21.1-52.0.1")
        setUpAndWait(processor, loadTemplate())
        assertEquals(ForgeVersions(v("26.1"), v("62.0.5")), forgeVersions())
    }

    @Test
    @DisplayName("Clearing the caches makes the next creator download versions again")
    fun clearCaches() = test {
        val template = loadTemplate()
        stubForge("1.20.1-47.1.0")
        setUpAndWait(processor, template)
        assertEquals(ForgeVersions(v("1.20.1"), v("47.1.0")), forgeVersions())

        stubForge("26.1-62.0.5")
        val cached = newProcessor()
        setUpAndWait(cached, template)
        assertEquals(ForgeVersions(v("1.20.1"), v("47.1.0")), forgeVersions(cached), "Without a reset")
        assertEquals(1, http.server.requestsTo(forgeUrl).size)

        CreatorCaches.clear()
        val fresh = newProcessor()
        setUpAndWait(fresh, template)
        assertEquals(ForgeVersions(v("26.1"), v("62.0.5")), forgeVersions(fresh), "After a reset")
        assertEquals(2, http.server.requestsTo(forgeUrl).size)
    }
}
