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

import com.demonwav.mcdev.util.fetchCatching
import com.demonwav.mcdev.util.fetchOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("fetchCatching and fetchOrNull Tests")
class FetchCatchingTest : FetcherTestBase() {

    private val logger = logger<FetchCatchingTest>()

    @Test
    @DisplayName("A successful fetch returns its result without logging")
    fun success() {
        assertEquals("ok", fetchOrNull(logger, "test") { "ok" })
        assertNothingLogged()
    }

    @Test
    @DisplayName("A failed fetch gives null and a warning")
    fun failure() {
        assertFailsQuietly { fetchOrNull(logger, "test") { throw IOException("boom") } }
    }

    @Test
    @DisplayName("A runtime exception gives null and a warning")
    fun runtimeFailure() {
        assertFailsQuietly { fetchOrNull(logger, "test") { throw IllegalStateException("boom") } }
    }

    @Test
    @DisplayName("Coroutine cancellation propagates")
    fun cancellation() {
        assertThrows<CancellationException> { fetchOrNull(logger, "test") { throw CancellationException() } }
        assertNothingLogged()
    }

    @Test
    @DisplayName("Progress cancellation propagates")
    fun processCanceled() {
        assertThrows<ProcessCanceledException> { fetchOrNull(logger, "test") { throw ProcessCanceledException() } }
        assertNothingLogged()
    }

    @Test
    @DisplayName("A failed catching fetch keeps its cause and logs a warning")
    fun catchingFailure() {
        val cause = IOException("boom")
        assertFailsQuietly { fetchCatching(logger, "test") { throw cause } }
        assertSame(cause, fetchCatching(logger, "test") { throw cause }.exceptionOrNull())
    }

    @Test
    @DisplayName("Coroutine cancellation propagates from a catching fetch")
    fun catchingCancellation() {
        assertThrows<CancellationException> { fetchCatching(logger, "test") { throw CancellationException() } }
        assertNothingLogged()
    }
}
