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

import com.demonwav.mcdev.creator.custom.CreatorSession
import com.demonwav.mcdev.creator.custom.TemplateService
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BareTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Creator Session Tests")
class CreatorSessionTest {

    private lateinit var fixture: BareTestFixture
    private lateinit var disposable: Disposable
    private lateinit var session: CreatorSession

    @BeforeEach
    fun setUp() {
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        fixture.setUp()
        disposable = Disposer.newDisposable(fixture.testRootDisposable, "creator session")
        session = TemplateService.instance.openSession("Creator Session Test", disposable)
    }

    @AfterEach
    fun tearDown() {
        fixture.tearDown()
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking { withTimeout(10.seconds) { block() } }

    @Test
    @DisplayName("A session with no work is idle")
    fun idleWithoutWork() = test {
        session.childScope("unused")
        assertTrue(session.isIdle)
        session.awaitIdle()
    }

    @Test
    @DisplayName("Work in a child scope keeps the session busy until it finishes")
    fun childScopeWork() = test {
        val gate = CompletableDeferred<Unit>()
        session.childScope("work").launch { gate.await() }
        assertFalse(session.isIdle)

        val waiter = async { session.awaitIdle() }
        assertFalse(waiter.isCompleted)
        gate.complete(Unit)
        waiter.await()
        assertTrue(session.isIdle)
    }

    @Test
    @DisplayName("Work launched directly in the main scope counts")
    fun mainScopeWork() = test {
        val gate = CompletableDeferred<Unit>()
        session.scope.launch { gate.await() }
        assertFalse(session.isIdle)
        gate.complete(Unit)
        session.awaitIdle()
        assertTrue(session.isIdle)
    }

    @Test
    @DisplayName("Waiting includes work launched by work that already finished")
    fun followUpWork() = test {
        val firstGate = CompletableDeferred<Unit>()
        val secondGate = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        session.childScope("first").launch {
            firstGate.await()
            session.childScope("second").launch {
                secondStarted.complete(Unit)
                secondGate.await()
            }
        }

        val waiter = async { session.awaitIdle() }
        firstGate.complete(Unit)
        secondStarted.await()
        assertFalse(waiter.isCompleted)
        assertFalse(session.isIdle)

        secondGate.complete(Unit)
        waiter.await()
        assertTrue(session.isIdle)
    }

    @Test
    @DisplayName("Disposing the session cancels its work and removes it from the active sessions")
    fun dispose() = test {
        assertTrue(session in TemplateService.instance.activeSessions)
        val job = session.childScope("forever").launch { awaitCancellation() }

        Disposer.dispose(disposable)

        job.join()
        assertTrue(job.isCancelled)
        assertTrue(session.isIdle)
        assertFalse(session in TemplateService.instance.activeSessions)
    }

    @Test
    @DisplayName("Sessions are listed oldest first")
    fun activeSessionsOrder() {
        val second = TemplateService.instance.openSession("Second", disposable)
        val sessions = TemplateService.instance.activeSessions
        assertEquals(listOf(session, second), sessions.filter { it === session || it === second })
    }
}
