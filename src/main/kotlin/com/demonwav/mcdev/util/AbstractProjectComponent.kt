/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project

abstract class AbstractProjectComponent(private var projectRef: Project?) : ProjectComponent {

    protected val project
        get() = projectRef!!

    override fun getComponentName(): String = javaClass.name

    final override fun disposeComponent() {
        projectRef = null
    }

    override fun initComponent() {}

    override fun projectClosed() {}
    override fun projectOpened() {}
}
