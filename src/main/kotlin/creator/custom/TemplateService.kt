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

package com.demonwav.mcdev.creator.custom

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.util.application
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Service
class TemplateService(private val scope: CoroutineScope) {

    private val sessions = CopyOnWriteArrayList<CreatorSession>()

    private val finalizers = MutableStateFlow<Map<String, ProjectFinalizers>>(emptyMap())

    /**
     * The sessions of every creator currently open, oldest first.
     */
    val activeSessions: List<CreatorSession>
        get() = sessions.toList()

    /**
     * Starts a session for a new creator. It's canceled and leaves [activeSessions] when [parentDisposable] is
     * disposed.
     */
    fun openSession(name: String, parentDisposable: Disposable): CreatorSession {
        val session = CreatorSession(name, scope)
        sessions += session
        Disposer.register(parentDisposable) {
            sessions -= session
            session.cancel()
        }
        return session
    }

    /**
     * Holds [action] until [executeFinalizer] is called for [project], which happens when the project opens. Only one
     * action can wait per project, later ones are dropped with an error.
     */
    fun registerFinalizerAction(project: Project, action: suspend () -> Unit) {
        val key = project.locationHash
        var registered = false
        finalizers.update { all ->
            val current = all[key] ?: ProjectFinalizers()
            registered = current.pending == null
            if (registered) all + (key to current.copy(pending = action)) else all
        }

        if (!registered) {
            thisLogger().error("More than one finalizer action registered for project $project")
        }
    }

    /**
     * Runs the action registered for [project], if there is one, and returns once it finishes.
     */
    suspend fun executeFinalizer(project: Project) {
        val key = project.locationHash
        // Count the action as running before it stops being pending, so isFinalizing never misses it
        val before = finalizers.getAndUpdate { all ->
            val current = all[key]
            if (current?.pending == null) all else all + (key to ProjectFinalizers(running = current.running + 1))
        }
        val action = before[key]?.pending ?: return

        try {
            action()
        } finally {
            finishedRunning(key)
        }
    }

    /**
     * Runs [action] in [scope] right away, tracked by [isFinalizing] and [awaitFinalizers] like a registered action.
     */
    fun launchFinalizer(project: Project, scope: CoroutineScope, action: suspend () -> Unit): Job {
        val key = project.locationHash
        finalizers.update { all ->
            val current = all[key] ?: ProjectFinalizers()
            all + (key to current.copy(running = current.running + 1))
        }

        // Counts down even if the scope is cancelled before the action starts
        return scope.launch { action() }.apply { invokeOnCompletion { finishedRunning(key) } }
    }

    /**
     * True while a finalize action for [project] is waiting for the project to open or still running.
     */
    fun isFinalizing(project: Project): Boolean = finalizers.value[project.locationHash] != null

    /**
     * Suspends until [isFinalizing] is false for [project]. An action waiting for the project to open keeps this
     * suspended until [executeFinalizer] runs it.
     */
    suspend fun awaitFinalizers(project: Project) {
        val key = project.locationHash
        finalizers.first { key !in it }
    }

    private fun finishedRunning(key: String) {
        finalizers.update { all ->
            val current = all[key] ?: return@update all
            val updated = current.copy(running = current.running - 1)
            if (updated.isDone) all - key else all + (key to updated)
        }
    }

    private data class ProjectFinalizers(val pending: (suspend () -> Unit)? = null, val running: Int = 0) {
        val isDone: Boolean
            get() = pending == null && running <= 0
    }

    companion object {

        val instance: TemplateService
            get() = application.service()
    }
}

class TemplateProjectFinalizerActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        TemplateService.instance.executeFinalizer(project)
    }
}
