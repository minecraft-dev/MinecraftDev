/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.intellij.openapi.project.Project

abstract class ProjectComponentHandler {

    open fun projectOpened(project: Project) {}
    open fun projectClosed(project: Project) {}
    open fun initComponent(project: Project) {}
    open fun disposeComponent(project: Project) {}
}
