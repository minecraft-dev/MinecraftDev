/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType

import com.intellij.openapi.module.Module

class MixinModule(module: Module) : AbstractModule(module) {

    override fun getModuleType() = MixinModuleType
    override fun getType() = PlatformType.MIXIN
    override fun getIcon() = null
}
