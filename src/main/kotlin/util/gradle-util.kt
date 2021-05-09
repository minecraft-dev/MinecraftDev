/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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
import java.util.concurrent.CountDownLatch
import org.jetbrains.plugins.gradle.util.GradleConstants

fun runGradleTask(project: Project, dir: Path, func: (ExternalSystemTaskExecutionSettings) -> Unit) {
    runGradleTaskWithCallback(project, dir, func, GradleCallback(null))
}

fun runGradleTaskAndWait(project: Project, dir: Path, func: (ExternalSystemTaskExecutionSettings) -> Unit) {
    val latch = CountDownLatch(1)

    runGradleTaskWithCallback(project, dir, func, GradleCallback(latch))

    latch.await()
}

private fun runGradleTaskWithCallback(
    project: Project,
    dir: Path,
    func: (ExternalSystemTaskExecutionSettings) -> Unit,
    callback: TaskCallback
) {
    val settings = ExternalSystemTaskExecutionSettings().apply {
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        externalProjectPath = dir.toString()
        func(this)
    }

    ExternalSystemUtil.runTask(
        settings,
        DefaultRunExecutor.EXECUTOR_ID,
        project,
        GradleConstants.SYSTEM_ID,
        callback,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
        false
    )
}

class GradleCallback(private val latch: CountDownLatch?) : TaskCallback {

    override fun onSuccess() = resume()
    override fun onFailure() = resume()

    private fun resume() {
        latch?.countDown()
    }
}
