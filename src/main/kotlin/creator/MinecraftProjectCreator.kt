/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Files
import java.nio.file.Path

class MinecraftProjectCreator {

    var buildSystem: BuildSystem? = null

    val configs = LinkedHashSet<ProjectConfig>()

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

                build.configure(configs, root)

                if (configs.size == 1) {
                    val config = configs.first()
                    val log = newLog(config, workLog)
                    if (!build.buildCreator(config, root, module).getSingleModuleSteps().run(indicator, log)) {
                        return
                    }
                    config.type.type.performCreationSettingSetup(module.project)
                    CreatorStep.runAllReformats()
                } else {
                    val types = configs.map { it.type }
                    newLog(build.javaClass.name + "::multiModuleBaseSteps", workLog).let { log ->
                        if (!build.multiModuleBaseSteps(module, types, root).run(indicator, log)) {
                            return
                        }
                    }

                    val postMultiModuleAwares = mutableListOf<PostMultiModuleAware>()

                    for (config in configs) {
                        val log = newLog(config, workLog)

                        val dirName = config.type.normalName.toLowerCase()
                        val newArtifactId = "${build.artifactId}-$dirName"
                        val dir = Files.createDirectories(root.resolve(newArtifactId))

                        val newBuild = build.createSub(newArtifactId)
                        val creator = newBuild.buildCreator(config, dir, module)
                        if (!creator.getMultiModuleSteps(root).run(indicator, log)) {
                            return
                        }
                        config.type.type.performCreationSettingSetup(module.project)
                        if (creator is PostMultiModuleAware) {
                            postMultiModuleAwares += creator
                        }
                    }

                    val commonArtifactId = "${build.artifactId}-common"
                    val commonDir = Files.createDirectories(root.resolve(commonArtifactId))
                    val commonBuild = build.createSub(commonArtifactId)

                    newLog(commonBuild.javaClass.name + "::multiModuleCommonSteps", workLog).let { log ->
                        if (!commonBuild.multiModuleCommonSteps(module, commonDir).run(indicator, log)) {
                            return
                        }
                    }

                    CreatorStep.runAllReformats()

                    newLog(build.javaClass.name + "::multiModuleBaseFinalizer", workLog).let { log ->
                        build.multiModuleBaseFinalizer(module, root).run(indicator, log)
                    }

                    for (postMultiModuleAware in postMultiModuleAwares) {
                        val log = newLog(postMultiModuleAware, workLog)
                        if (!postMultiModuleAware.getPostMultiModuleSteps(root).run(indicator, log)) {
                            return
                        }
                    }
                }

                // Tell IntelliJ about everything we've done
                invokeLater {
                    VfsUtil.markDirtyAndRefresh(false, true, true, root.virtualFileOrError)
                }
            } catch (e: Exception) {
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
