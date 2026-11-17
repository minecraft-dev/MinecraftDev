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

import com.demonwav.mcdev.creator.custom.TemplateService
import com.demonwav.mcdev.framework.LogCapture
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Template Service Tests")
class TemplateServiceTest {

    private lateinit var fixture: IdeaProjectTestFixture
    private val project: Project
        get() = fixture.project
    private val service: TemplateService
        get() = TemplateService.instance

    @BeforeEach
    fun setUp() {
        fixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createLightFixtureBuilder(null, "mcdev_template_service_test")
            .fixture
        // Light fixtures must be set up and torn down on the EDT
        runInEdtAndWait { fixture.setUp() }
    }

    @AfterEach
    fun tearDown() {
        try {
            // Don't leave a pending action behind for a later test using the same light project
            runBlocking { service.executeFinalizer(project) }
        } finally {
            runInEdtAndWait { fixture.tearDown() }
        }
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking { withTimeout(10.seconds) { block() } }

    @Test
    @DisplayName("A registered action waits for executeFinalizer")
    fun registeredAction() = test {
        val ran = CompletableDeferred<Unit>()
        service.registerFinalizerAction(project) { ran.complete(Unit) }
        assertTrue(service.isFinalizing(project))
        assertFalse(ran.isCompleted)

        service.executeFinalizer(project)

        assertTrue(ran.isCompleted)
        assertFalse(service.isFinalizing(project))
    }

    @Test
    @DisplayName("Executing with nothing registered does nothing")
    fun nothingRegistered() = test {
        service.executeFinalizer(project)
        assertFalse(service.isFinalizing(project))
    }

    @Test
    @DisplayName("isFinalizing stays true while the action runs")
    fun runningAction() = test {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        service.registerFinalizerAction(project) {
            started.complete(Unit)
            gate.await()
        }

        val execution = async { service.executeFinalizer(project) }
        started.await()
        assertTrue(service.isFinalizing(project))

        val waiter = async { service.awaitFinalizers(project) }
        assertFalse(waiter.isCompleted)

        gate.complete(Unit)
        execution.await()
        waiter.await()
        assertFalse(service.isFinalizing(project))
    }

    @Test
    @DisplayName("A second registration for the same project logs an error and keeps the first")
    fun duplicateRegistration() = test {
        val ran = CopyOnWriteArrayList<String>()
        service.registerFinalizerAction(project) { ran += "first" }

        val errors = LogCapture().use { capture ->
            service.registerFinalizerAction(project) { ran += "second" }
            capture.errors.toList()
        }
        assertEquals(1, errors.size, "Expected one logged error, got $errors")
        assertTrue("More than one finalizer action" in errors.single(), errors.single())

        service.executeFinalizer(project)
        assertEquals(listOf("first"), ran)
    }

    @Test
    @DisplayName("A launched action is tracked until it finishes")
    fun launchedAction() = test {
        val gate = CompletableDeferred<Unit>()
        val job = service.launchFinalizer(project, this) { gate.await() }
        assertTrue(service.isFinalizing(project))

        val waiter = async { service.awaitFinalizers(project) }
        assertFalse(waiter.isCompleted)

        gate.complete(Unit)
        job.join()
        waiter.await()
        assertFalse(service.isFinalizing(project))
    }

    @Test
    @DisplayName("A launched action cancelled before it starts stops being tracked")
    fun launchedActionCancelled() = test {
        val scope = CoroutineScope(Job())
        scope.cancel()
        val job = service.launchFinalizer(project, scope) { error("Should not run") }
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(service.isFinalizing(project))
    }

    @Test
    @DisplayName("Pending and running actions are both tracked")
    fun pendingAndLaunched() = test {
        val gate = CompletableDeferred<Unit>()
        service.registerFinalizerAction(project) {}
        val job = service.launchFinalizer(project, this) { gate.await() }

        service.executeFinalizer(project)
        assertTrue(service.isFinalizing(project), "The launched action is still running")

        gate.complete(Unit)
        job.join()
        assertFalse(service.isFinalizing(project))
    }
}
