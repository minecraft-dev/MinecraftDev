/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class QuiltFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        project ?: return null

        if (!MinecraftSettings.instance.isShowProjectPlatformIcons) {
            return null
        }

        val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
        val quiltModule = MinecraftFacet.getInstance(module, QuiltModuleType) ?: return null

        if (file == quiltModule.quiltJson) {
            return quiltModule.icon
        }

        return null
    }
}
