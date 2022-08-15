/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.exception.ProjectCreatorException
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.virtualFileOrError
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Path

class MinecraftProjectCreator {

    var buildSystem: BuildSystem? = null

    var config: ProjectConfig? = null

    fun create(root: Path, module: Module) {
        val build = buildSystem ?: throw IllegalStateException("buildSystem not initialized")
        ProgressManager.getInstance().run(CreateTask(root, module, build))
    }

    class WorkLogStep(val config: Any) {
        private val steps = mutableListOf<Pair<Any, Int>>()
        private var currentStep: Pair<Any, Int>? = null

        fun printState(sb: StringBuilder) {
            sb.append("    ").appendLine(if (config is String) config else config.javaClass.name)
            for ((step, indent) in steps) {
                printStep(sb, step, "        ", indent)
            }
            currentStep?.let { (step, indent) ->
                printStep(sb, step, "      > ", indent)
            }
        }

        private fun printStep(sb: StringBuilder, step: Any, baseIndent: String, indent: Int) {
            repeat(indent) {
                sb.append("    ")
            }
            sb.append(baseIndent).appendLine(if (step is String) step else step.javaClass.name)
        }

        fun newCurrentStep(newStep: Any, indent: Int = 0) {
            finishCurrentStep()
            currentStep = newStep to indent
        }

        fun finishCurrentStep() {
            currentStep?.let { step ->
                steps += step
            }
            currentStep = null
        }

        companion object {
            var currentLog: WorkLogStep? = null
        }
    }

    private inner class CreateTask(
        private val root: Path,
        private val module: Module,
        private val build: BuildSystem
    ) : Task.Backgroundable(module.project, "Setting up project", false) {
        override fun shouldStartInBackground() = false

        override fun run(indicator: ProgressIndicator) {
            if (module.isDisposed || project.isDisposed) {
                return
            }

            val workLog = mutableListOf<WorkLogStep>()

            try {
                // Should be empty, just make sure IntelliJ knows that
                invokeAndWait {
                    VfsUtil.markDirtyAndRefresh(false, true, true, root.virtualFileOrError)
                }

                val config = this@MinecraftProjectCreator.config ?: return

                build.configure(config, root)

                val log = newLog(config, workLog)
                if (!build.buildCreator(config, root, module).getSteps().run(indicator, log)) {
                    return
                }
                config.type.type.performCreationSettingSetup(module.project)
                CreatorStep.runAllReformats()

                // Tell IntelliJ about everything we've done
                invokeLater {
                    VfsUtil.markDirtyAndRefresh(false, true, true, root.virtualFileOrError)
                }
            } catch (e: Exception) {
                if (e is ProcessCanceledException || e.cause is ProcessCanceledException) {
                    // Do not log PCE. The second condition is there because LaterInvocator wraps PCEs in RuntimeExceptions
                    return
                }
                val workLogText = buildString {
                    appendLine("Build steps completed:")
                    for (workLogStep in workLog) {
                        workLogStep.printState(this)
                    }
                }
                throw ProjectCreatorException(workLogText, e)
            } finally {
                WorkLogStep.currentLog = null
            }
        }

        private fun Iterable<CreatorStep>.run(indicator: ProgressIndicator, workLog: WorkLogStep): Boolean {
            for (step in this) {
                if (module.isDisposed || project.isDisposed) {
                    return false
                }
                workLog.newCurrentStep(step)
                step.runStep(indicator)
            }
            workLog.finishCurrentStep()
            return true
        }

        private fun newLog(obj: Any, workLog: MutableList<WorkLogStep>): WorkLogStep {
            val log = WorkLogStep(obj)
            WorkLogStep.currentLog = log
            workLog += log
            return log
        }
    }
}
