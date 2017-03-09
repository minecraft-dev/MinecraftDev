/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.mapNotNull
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.stream
import javax.swing.Icon

class BukkitFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        if (!MinecraftSettings.instance.isShowProjectPlatformIcons) {
            return null
        }

        project ?: return null

        return ModuleManager.getInstance(project).modules.stream()
            .mapNotNull { MinecraftFacet.getInstance<BukkitModule<*>>(it, BukkitModuleType, SpigotModuleType, PaperModuleType) }
            .filter { file == it.pluginYml }
            .findFirst()
            .map { it.icon }
            .orElse(null)
    }
}
