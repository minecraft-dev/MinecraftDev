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

import com.demonwav.mcdev.buildsystem.BuildSystemInstanceManager
import com.intellij.openapi.module.Module

object TestBuildSystemInstanceManager : BuildSystemInstanceManager {
    override fun getBuildSystem(module: Module) = TestBuildSystem
}
