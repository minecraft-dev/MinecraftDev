/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

import com.intellij.openapi.module.Module

interface BuildSystemInstanceManager {

    fun getBuildSystem(module: Module): BuildSystem?
}
