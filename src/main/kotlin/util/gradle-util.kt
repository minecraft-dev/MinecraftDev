/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.jetbrains.plugins.gradle.util.GradleConstants

inline fun runGradleTask(
    project: Project,
    dir: Path,
    func: (ExternalSystemTaskExecutionSettings) -> Unit
) {
    val settings = ExternalSystemTaskExecutionSettings().apply {
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        externalProjectPath = dir.toString()
        func(this)
    }

    val lock = ReentrantLock()
    val condition = lock.newCondition()

    lock.withLock {
        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            GradleCallback(lock, condition),
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false
        )

        condition.await()
    }
}

class GradleCallback(private val lock: ReentrantLock, private val condition: Condition) : TaskCallback {

    override fun onSuccess() = resume()
    override fun onFailure() = resume()

    private fun resume() {
        lock.withLock {
            condition.signalAll()
        }
    }
}
