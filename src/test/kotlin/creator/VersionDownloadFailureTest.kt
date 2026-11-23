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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.CreatorTemplateProcessor
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.TemplateService
import com.demonwav.mcdev.creator.custom.TemplateValidationReporterImpl
import com.demonwav.mcdev.creator.custom.model.ForgeVersions
import com.demonwav.mcdev.creator.custom.model.NeoForgeVersions
import com.demonwav.mcdev.creator.custom.types.ArchitecturyVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.FabricGameVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.FabricVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.ForgeVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.GradlePluginSelectorCreatorProperty
import com.demonwav.mcdev.creator.custom.types.MavenArtifactVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.NeoForgeVersionsCreatorProperty
import com.demonwav.mcdev.creator.custom.types.PaperVersionCreatorProperty
import com.demonwav.mcdev.creator.custom.types.ParchmentCreatorProperty
import com.demonwav.mcdev.creator.custom.types.VersionDownloadStatus
import com.demonwav.mcdev.framework.LogCapture
import com.demonwav.mcdev.framework.http.HttpMockExtension
import com.demonwav.mcdev.framework.http.HttpMockServer.Companion.mockUrl
import com.demonwav.mcdev.util.SemanticVersion
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.observable.properties.PropertyGraph
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@DisplayName("Version Download Failure Tests")
class VersionDownloadFailureTest : CreatorTemplateProcessorTestBase() {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    private lateinit var log: LogCapture

    private val forgeUrl = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml"
    private val neoForgeUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    private val modDevUrl =
        "https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml"
    private val fabricMetaUrl = "https://meta.fabricmc.net/v2/versions"
    private val fabricLoomUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml"
    private val architecturyApiUrl = "https://api.modrinth.com/v2/project/architectury-api/version"
    private val parchmentUrl = "https://maven.parchmentmc.org/org/parchmentmc/data/"
    private val paperUrl = "https://fill.papermc.io/v3/projects/paper"
    private val gremlinUrl = "https://plugins.gradle.org/m2/xyz/jpenilla/gremlin-gradle/maven-metadata.xml"

    private val failureMessage = MCDevBundle("creator.ui.versions_download.failed")
    private val serverError = "Request failed with status code 500"
    private val serverErrorMessage = MCDevBundle("creator.ui.versions_download.failed_cause", serverError)
    private val noneFoundMessage = MCDevBundle("creator.ui.versions_download.none_found")
    private val noMatchParameters = """{"sourceUrl": "$gremlinUrl", "rawVersionFilter": "false"}"""

    private fun partFailedMessage(part: String) =
        MCDevBundle("creator.ui.versions_download.failed_part", part, serverError)

    @BeforeEach
    override fun setUp() {
        super.setUp()
        log = LogCapture()
    }

    @AfterEach
    override fun tearDown() {
        try {
            log.close()
            log.assertNoErrors()
        } finally {
            super.tearDown()
        }
    }

    private fun test(block: suspend () -> Unit) = runBlocking { withTimeout(30.seconds) { block() } }

    private fun template(type: String, parameters: String = "{}") = """
        {
          "version": ${TemplateDescriptor.LATEST_FORMAT_VERSION},
          "properties": [
            {
              "name": "VERSIONS",
              "type": "$type",
              "parameters": $parameters
            }
          ]
        }
    """.trimIndent()

    private fun fail(vararg urls: String) {
        for (url in urls) {
            http.server.stubFor(get(mockUrl(url)).willReturn(serverError()))
        }
    }

    private fun restore(vararg urls: String) {
        for (url in urls) {
            http.server.stubFor(get(mockUrl(url)).willReturn(http.server.fixture(url)))
        }
    }

    private fun newProcessor(): CreatorTemplateProcessor {
        val graph = PropertyGraph()
        val context = WizardContext(null, fixture.testRootDisposable)
        context.overrideModalityState(ModalityState.nonModal())
        val session = TemplateService.instance.openSession("Second creator", fixture.testRootDisposable)
        return CreatorTemplateProcessor(graph, context, session, TestExternalTemplatePropertyProvider(graph))
            .apply { initBuiltinProperties() }
    }

    private suspend fun setUpAndWait(template: String, processor: CreatorTemplateProcessor = this.processor) {
        val reporter = TemplateValidationReporterImpl()
        processor.setupTemplate(TestLoadedTemplate(template), reporter)
        assertFalse(reporter.hasErrors, "Template errors: ${reporter.items}")
        processor.context.session.awaitIdle()
    }

    private fun status(processor: CreatorTemplateProcessor = this.processor): VersionDownloadStatus {
        return when (val property = processor.context.properties["VERSIONS"]) {
            is ArchitecturyVersionsCreatorProperty -> property.downloadStatus
            is FabricGameVersionCreatorProperty -> property.downloadStatus
            is FabricVersionsCreatorProperty -> property.downloadStatus
            is ForgeVersionsCreatorProperty -> property.downloadStatus
            is GradlePluginSelectorCreatorProperty -> property.downloadStatus
            is MavenArtifactVersionCreatorProperty -> property.downloadStatus
            is NeoForgeVersionsCreatorProperty -> property.downloadStatus
            is PaperVersionCreatorProperty -> property.downloadStatus
            is ParchmentCreatorProperty -> property.downloadStatus
            else -> throw AssertionError("No download status for $property")
        }
    }

    private fun assertFailed(
        processor: CreatorTemplateProcessor = this.processor,
        message: String = failureMessage,
    ) {
        assertEquals(message, status(processor).failure.get())
        assertTrue(log.warnings.isNotEmpty(), "The failure should be logged as a warning")
        log.assertNoErrors()
    }

    private fun assertSucceeded(processor: CreatorTemplateProcessor = this.processor) {
        assertEquals("", status(processor).failure.get())
        assertFalse(status(processor).loading.get())
    }

    private fun assertFailsWith(
        type: String,
        vararg urls: String,
        parameters: String = "{}",
        message: String = failureMessage,
    ) = test {
        fail(*urls)
        setUpAndWait(template(type, parameters))
        assertFailed(message = message)
    }

    private fun assertFindsNone(type: String) = test {
        setUpAndWait(template(type, noMatchParameters))
        assertFailed(message = noneFoundMessage)
    }

    @Test
    @DisplayName("Forge versions show a failure")
    fun forge() = assertFailsWith("forge_versions", forgeUrl, message = serverErrorMessage)

    @Test
    @DisplayName("NeoForge versions show a failure when one part fails")
    fun neoForge() = assertFailsWith("neoforge_versions", modDevUrl, message = partFailedMessage("ModDevGradle"))

    @Test
    @DisplayName("Fabric versions show a failure when one part fails")
    fun fabric() = assertFailsWith("fabric_versions", fabricLoomUrl, message = partFailedMessage("Fabric Loom"))

    @Test
    @DisplayName("Fabric game versions show a failure")
    fun fabricGame() = assertFailsWith("fabric_game_version", fabricMetaUrl, message = serverErrorMessage)

    @Test
    @DisplayName("Architectury versions show a failure when one part fails")
    fun architectury() = assertFailsWith(
        "architectury_versions",
        architecturyApiUrl,
        message = partFailedMessage("Architectury API"),
    )

    @Test
    @DisplayName("Parchment versions show a failure")
    fun parchment() = assertFailsWith("parchment", parchmentUrl)

    @Test
    @DisplayName("Paper versions show a failure")
    fun paper() = assertFailsWith("paper_versions", paperUrl)

    @Test
    @DisplayName("Maven artifact versions show a failure")
    fun mavenArtifact() = assertFailsWith(
        "maven_artifact_version",
        gremlinUrl,
        parameters = """{"sourceUrl": "$gremlinUrl"}""",
        message = serverErrorMessage,
    )

    @Test
    @DisplayName("Gradle plugin versions show a failure")
    fun gradlePlugin() = assertFailsWith(
        "gradle_plugin",
        gremlinUrl,
        parameters = """{"sourceUrl": "$gremlinUrl"}""",
        message = serverErrorMessage,
    )

    @Test
    @DisplayName("Maven artifact versions show a failure when no version matches the filters")
    fun mavenArtifactNoneFound() = assertFindsNone("maven_artifact_version")

    @Test
    @DisplayName("Gradle plugin versions show a failure when no version matches the filters")
    fun gradlePluginNoneFound() = assertFindsNone("gradle_plugin")

    @Test
    @DisplayName("A later creator downloads versions again when none matched the filters")
    fun retryNoneFound() = test {
        val template = template("maven_artifact_version", noMatchParameters)
        setUpAndWait(template)
        assertFailed(message = noneFoundMessage)

        val retry = newProcessor()
        setUpAndWait(template, retry)
        assertFailed(retry, noneFoundMessage)
        assertEquals(2, http.server.requestsTo(gremlinUrl).size)
    }

    @Test
    @DisplayName("A later creator downloads versions again after a failure")
    fun retry() = test {
        val template = template("forge_versions")
        fail(forgeUrl)
        setUpAndWait(template)
        assertFailed(message = serverErrorMessage)

        restore(forgeUrl)
        val retry = newProcessor()
        setUpAndWait(template, retry)
        assertSucceeded(retry)
        assertEquals(
            ForgeVersions(SemanticVersion.parse("26.3"), SemanticVersion.parse("66.1.8")),
            retry.context.property<ForgeVersions>("VERSIONS").get(),
        )
        assertEquals(2, http.server.requestsTo(forgeUrl).size)
    }

    @Test
    @DisplayName("A later creator downloads only the parts that failed")
    fun retryPart() = test {
        val template = template("neoforge_versions")
        fail(modDevUrl)
        setUpAndWait(template)
        assertFailed(message = partFailedMessage("ModDevGradle"))

        restore(modDevUrl)
        val retry = newProcessor()
        setUpAndWait(template, retry)
        assertSucceeded(retry)
        val versions = checkNotNull(retry.context.property<NeoForgeVersions>("VERSIONS").get())
        assertEquals(SemanticVersion.parse("26.3"), versions.minecraft)
        assertEquals(SemanticVersion.parse("2.0.148"), versions.moddev)
        assertEquals(1, http.server.requestsTo(neoForgeUrl).size)
        assertEquals(2, http.server.requestsTo(modDevUrl).size)
    }
}
