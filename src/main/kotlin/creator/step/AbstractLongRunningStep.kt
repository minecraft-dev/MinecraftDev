/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import java.util.concurrent.ConcurrentLinkedQueue

private typealias TaskQueue = ConcurrentLinkedQueue<AbstractLongRunningStep>

abstract class AbstractLongRunningStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    abstract val description: String

    abstract fun perform(project: Project)

    final override fun setupProject(project: Project) {
        val newQueue = TaskQueue()
        val queue = (data as UserDataHolderEx).putUserDataIfAbsent(TASK_QUEUE_KEY, newQueue)
        queue += this
        if (queue === newQueue) {
            startTaskQueue(project, queue)
        }
    }

    private fun startTaskQueue(project: Project, queue: TaskQueue) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Your project is being created") {
            override fun run(indicator: ProgressIndicator) {
                if (project.isDisposed) {
                    return
                }

                indicator.text = "Your project is being created"
                var currentQueue = queue
                while (true) {
                    while (true) {
                        val task = currentQueue.poll() ?: break
                        indicator.text2 = task.description
                        if (project.isDisposed) {
                            return
                        }
                        task.perform(project)
                        if (project.isDisposed) {
                            return
                        }
                    }
                    if ((data as UserDataHolderEx).replace(TASK_QUEUE_KEY, currentQueue, null)) {
                        break
                    }
                    currentQueue = data.getUserData(TASK_QUEUE_KEY) ?: break
                }
                indicator.text2 = null
            }
        })
    }

    companion object {
        private val TASK_QUEUE_KEY = Key.create<TaskQueue>("${AbstractLongRunningStep::class.java.name}.queue")
    }
}
