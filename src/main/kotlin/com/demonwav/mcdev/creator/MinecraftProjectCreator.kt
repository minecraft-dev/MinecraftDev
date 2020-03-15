/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile

class MinecraftProjectCreator {

    var buildSystem: BuildSystem? = null

    val configs = LinkedHashSet<ProjectConfiguration>()

    fun create(root: VirtualFile, module: Module) {
        val build = buildSystem ?: return

        if (configs.size == 1) {
            doSingleModuleCreate(root, build, module)
        } else {
            doMultiModuleCreate(root, build, module)
        }
    }

    private fun doSingleModuleCreate(rootDirectory: VirtualFile, buildSystem: BuildSystem, module: Module) {
        if (configs.isEmpty()) {
            return
        }
        val configuration = configs.first()
        configuration.setupDependencies(buildSystem)

        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                if (module.isDisposed || module.project.isDisposed) {
                    return
                }
                indicator.isIndeterminate = true

                val pluginName = configuration.base?.pluginName ?: return
                buildSystem.create(module.project, rootDirectory, configuration, indicator, pluginName)
                configuration.create(module.project, buildSystem, indicator)
                configuration.type.type.performCreationSettingSetup(module.project)
                buildSystem.finishSetup(module, rootDirectory, configs, indicator)
            }
        })
    }

    private fun doMultiModuleCreate(rootDirectory: VirtualFile, buildSystem: BuildSystem, module: Module) {
        if (buildSystem !is GradleBuildSystem) {
            throw IllegalStateException("BuildSystem must be Gradle")
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(module.project, "Setting Up Project", false) {
            override fun shouldStartInBackground() = false

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val pluginName = configs.firstOrNull()?.base?.pluginName ?: return
                val map = buildSystem.createMultiModuleProject(
                    rootDirectory, module.project, configs, indicator, pluginName
                )

                map.forEach { (g, p) ->
                    p.create(module.project, g, indicator)
                    p.type.type.performCreationSettingSetup(module.project)
                }
                buildSystem.finishSetup(module, rootDirectory, map.values, indicator)
            }
        })
    }
}
