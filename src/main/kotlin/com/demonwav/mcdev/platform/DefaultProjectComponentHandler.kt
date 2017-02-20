/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.ProjectComponentHandler
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager

object DefaultProjectComponentHandler : ProjectComponentHandler() {

    override fun projectOpened(project: Project) {
        StartupManager.getInstance(project).registerPostStartupActivity {
            ModuleManager.getInstance(project).modules.forEach {
                MinecraftModule.getInstance(it)
            }
        }
    }
}
