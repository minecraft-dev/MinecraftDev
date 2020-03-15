/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.debug

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.util.ModuleDebugRunConfigurationExtension
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.module.Module

class MixinRunConfigurationExtension : ModuleDebugRunConfigurationExtension() {

    override fun attachToProcess(handler: ProcessHandler, module: Module) {
        if (MinecraftFacet.getInstance(module)?.isOfType(MixinModuleType) == true) {
            // Add marker data to enable Mixin debugger
            handler.putUserData(MIXIN_DEBUG_KEY, true)
        }
    }
}
