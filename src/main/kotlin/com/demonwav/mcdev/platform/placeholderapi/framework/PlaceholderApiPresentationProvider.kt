/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.placeholderapi.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.placeholderapi.util.PlaceholderApiConstants
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile

class PlaceholderApiPresentationProvider :
    LibraryPresentationProvider<LibraryVersionProperties>(PLACEHOLDERAPI_LIBRARY_KIND) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.PLACEHOLDERAPI_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = classesRoot.localFile

            if (JarUtil.containsClass(file, PlaceholderApiConstants.EXPANSION_CLASS)) {
                return LibraryVersionProperties()
            }
        }
        return null
    }
}
