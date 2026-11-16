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

package com.demonwav.mcdev.framework.http

import java.nio.file.Path
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Starts an [HttpMockServer] before each test and routes the plugin's HTTP traffic to it through
 * [HttpMockServer.MOCK_PROPERTY]. After each test it restores the property, stops the server, and fails the test if the
 * server saw any problems, such as a request with no fixture.
 *
 * The property is JVM-wide, so tests using this extension can't run in parallel.
 *
 * ```
 * @JvmField
 * @RegisterExtension
 * val http = HttpMockExtension()
 *
 * @Test
 * fun test() {
 *     http.server.stubFor(get(mockUrl("https://fill.papermc.io/v3/projects/paper")).willReturn(serverError()))
 * }
 * ```
 */
class HttpMockExtension(private val fixtureRoots: () -> List<Path> = HttpFixtures::defaultRoots) :
    BeforeEachCallback, AfterEachCallback {

    private var current: HttpMockServer? = null
    private var previousProperty: String? = null

    val server: HttpMockServer
        get() = checkNotNull(current) { "The HTTP mock server only runs during a test" }

    override fun beforeEach(context: ExtensionContext) {
        val server = HttpMockServer(fixtureRoots())
        current = server
        previousProperty = System.getProperty(HttpMockServer.MOCK_PROPERTY)
        System.setProperty(HttpMockServer.MOCK_PROPERTY, server.baseUrl)
    }

    override fun afterEach(context: ExtensionContext) {
        val server = current ?: return
        current = null
        val previous = previousProperty
        if (previous == null) {
            System.clearProperty(HttpMockServer.MOCK_PROPERTY)
        } else {
            System.setProperty(HttpMockServer.MOCK_PROPERTY, previous)
        }
        server.close()
        server.verify()
    }
}
