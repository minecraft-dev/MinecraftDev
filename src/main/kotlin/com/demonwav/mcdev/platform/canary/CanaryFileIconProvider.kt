/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class CanaryFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, @Iconable.IconFlags flags: Int, project: Project?): Icon? {
        project ?: return null

        if (!MinecraftSettings.instance.isShowProjectPlatformIcons) {
            return null
        }

        val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
        val canaryModule = MinecraftFacet.getInstance(module, CanaryModuleType, NeptuneModuleType) ?: return null

        if (file == canaryModule.canaryInf || file == canaryModule.neptuneInf) {
            return canaryModule.icon
        }
        return null
    }
}
