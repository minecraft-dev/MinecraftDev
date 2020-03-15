/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ConfigurePluginUpdatesAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ConfigurePluginUpdatesDialog().show()
    }
}
