/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.transition

import com.demonwav.mcdev.util.AbstractProjectComponent
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

class TransitionProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
        // Reset all Modules back to JavaModuleType
        for (module in ModuleManager.getInstance(project).modules) {
            for (type in types) {
                // This only exists for legacy reasons, to reset the type option (previous versions used to set it).
                // It's not actually used anymore, so suppress deprecation
                @Suppress("DEPRECATION")
                if (module.getOptionValue("type") == type) {
                    module.setOption("type", JavaModuleType.getModuleType().id)
                }
            }
        }
    }

    companion object {
        private val types = arrayOf(
            "BUKKIT_MODULE_TYPE",
            "SPIGOT_MODULE_TYPE",
            "PAPER_MODULE_TYPE",
            "SPONGE_MODULE_TYPE",
            "FORGE_MODULE_TYPE",
            "BUNGEECORD_MODULE_TYPE"
        )
    }
}
