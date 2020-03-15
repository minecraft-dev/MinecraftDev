/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile

class ForgePresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(FORGE_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.FORGE_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            if (JarUtil.containsClass(classesRoot.localFile, ForgeConstants.MOD_ANNOTATION)) {
                return LibraryVersionProperties()
            }
        }
        return null
    }
}
