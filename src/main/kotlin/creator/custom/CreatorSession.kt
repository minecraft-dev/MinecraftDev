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

import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll

/**
 * The coroutine scopes of one open creator. Callers can check whether the creator still has background work running,
 * such as version downloads, through [isIdle] and [awaitIdle].
 *
 * Get one from [TemplateService.openSession].
 */
class CreatorSession internal constructor(val name: String, parentScope: CoroutineScope) {

    /**
     * The creator's main scope. Cancelled when the creator closes.
     */
    val scope: CoroutineScope = parentScope.childScope(name)

    // Guarded by itself. A new scope joins its parent before it's added here, and without the lock a concurrent
    // check would see it as running work that never finishes.
    private val scopeJobs: MutableSet<Job> = hashSetOf(scope.coroutineContext.job)

    /**
     * A scope under [scope] whose work counts towards [isIdle].
     */
    fun childScope(name: String): CoroutineScope {
        synchronized(scopeJobs) {
            val child = scope.childScope(name)
            scopeJobs += child.coroutineContext.job
            return child
        }
    }

    /**
     * True if no coroutine launched in this session's scopes is still running.
     */
    val isIdle: Boolean
        get() = runningJobs().isEmpty()

    /**
     * Suspends until [isIdle] is true, including work launched while waiting. Never returns if called from a coroutine
     * running in this session.
     */
    suspend fun awaitIdle() {
        while (true) {
            val running = runningJobs()
            if (running.isEmpty()) {
                return
            }
            running.joinAll()
        }
    }

    internal fun cancel() {
        scope.cancel("The creator got disposed")
    }

    private fun runningJobs(): List<Job> = synchronized(scopeJobs) {
        buildList { collectRunningJobs(scope.coroutineContext.job, this) }
    }

    // Scope jobs never complete on their own, so look through them at the work launched in them
    private fun collectRunningJobs(parent: Job, into: MutableList<Job>) {
        for (child in parent.children) {
            if (child in scopeJobs) {
                collectRunningJobs(child, into)
            } else if (!child.isCompleted) {
                into += child
            }
        }
    }
}
