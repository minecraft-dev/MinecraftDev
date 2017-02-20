/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework.buildsystem

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

object TestBuildSystem : BuildSystem() {

    override fun create(project: Project, configurations: ProjectConfiguration, indicator: ProgressIndicator) {}

    override fun finishSetup(module: Module, configurations: Collection<ProjectConfiguration>, indicator: ProgressIndicator) {}

    /**
     * At test time, there is nothing to import, so instead, just return ourselves immediately
     */
    override fun reImport(module: Module): Promise<out TestBuildSystem> {
        return resolvedPromise(this)
    }
}
