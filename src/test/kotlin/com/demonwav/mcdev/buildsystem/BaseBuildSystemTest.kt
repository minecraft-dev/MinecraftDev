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

import com.demonwav.mcdev.framework.BaseMinecraftTestCase
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel

abstract class BaseBuildSystemTest : BaseMinecraftTestCase(MixinModuleType) {

    init {
        buildSystemInstanceManager = DefaultBuildSystemInstanceManager
    }

    override fun preConfigureModule(module: Module, model: ModifiableRootModel) {
        // DefaultBuildSystemInstanceManager uses the module's content root to search for the file, so we must set it
        model.addContentEntry(project.baseDir)
    }
}
