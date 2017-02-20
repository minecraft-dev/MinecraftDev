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

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class MinecraftProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        ProjectComponentManager.handler.projectOpened(myProject)
    }

    override fun projectClosed() {
        ProjectComponentManager.handler.projectClosed(myProject)
    }

    override fun initComponent() {
        ProjectComponentManager.handler.initComponent(myProject)
    }

    override fun disposeComponent() {
        ProjectComponentManager.handler.disposeComponent(myProject)
    }
}
