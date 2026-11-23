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

package com.demonwav.mcdev.creator.fetcher

import com.demonwav.mcdev.framework.LogCapture
import com.demonwav.mcdev.framework.http.HttpMockExtension
import com.demonwav.mcdev.framework.http.HttpMockServer.Companion.mockUrl
import com.demonwav.mcdev.util.NoVersionsFoundException
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.intellij.testFramework.fixtures.BareTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Runs fetchers against the recorded fixtures, capturing what they log. Every test fails if a fetcher logs an error.
 */
abstract class FetcherTestBase {

    @JvmField
    @RegisterExtension
    val http = HttpMockExtension()

    // The plugin's user agent reads the plugin version, which needs the IDE application
    private lateinit var fixture: BareTestFixture
    lateinit var log: LogCapture

    @BeforeEach
    fun setUpFetcherTest() {
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()
        log = LogCapture()
    }

    @AfterEach
    fun tearDownFetcherTest() {
        try {
            log.close()
            log.assertNoErrors()
        } finally {
            fixture.tearDown()
        }
    }

    fun <T> fetch(block: suspend () -> T): T = runBlocking { withTimeout(30.seconds) { block() } }

    fun stub(url: String, response: ResponseDefinitionBuilder) {
        http.server.stubFor(get(mockUrl(url)).willReturn(response))
    }

    fun stubBody(url: String, body: String) = stub(url, ok(body))

    fun stubServerError(url: String) = stub(url, serverError())

    fun stubNotFound(url: String) = stub(url, notFound())

    fun mavenMetadata(vararg versions: String): String {
        val tags = versions.joinToString("") { "<version>$it</version>" }
        return "<metadata><versioning><versions>$tags</versions></versioning></metadata>"
    }

    /**
     * Asserts that [fetcher] returns null or a failure other than finding no versions, and logs a warning without an
     * error.
     */
    fun assertFailsQuietly(fetcher: suspend () -> Any?) {
        val result = fetch(fetcher)
        assertTrue(
            result == null || (result is Result<*> && result.isFailure),
            "The fetcher should give null or a failure, but gave $result",
        )
        assertFalse(
            result is Result<*> && result.exceptionOrNull() is NoVersionsFoundException,
            "The fetcher should fail, not find no versions",
        )
        assertTrue(log.warnings.isNotEmpty(), "The failure should be logged as a warning")
        log.assertNoErrors()
    }

    /**
     * Asserts that [fetcher] reports finding no versions and logs a warning without an error.
     */
    fun assertFindsNone(fetcher: suspend () -> Result<*>) {
        val failure = fetch(fetcher).exceptionOrNull()
        assertInstanceOf(NoVersionsFoundException::class.java, failure, "The fetcher should find no versions")
        assertTrue(log.warnings.isNotEmpty(), "Finding no versions should be logged as a warning")
        log.assertNoErrors()
    }

    /**
     * Asserts that nothing was logged as a warning or an error.
     */
    fun assertNothingLogged() {
        assertEquals(emptyList<String>(), log.warnings)
        log.assertNoErrors()
    }

    companion object {
        const val MALFORMED_XML = "<metadata><versioning><versions><version>1.0</versions>"
        const val MALFORMED_JSON = "{\"versions\": [1, 2"
    }
}
