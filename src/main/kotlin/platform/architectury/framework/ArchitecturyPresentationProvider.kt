/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.JarFile

class ArchitecturyPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(
    ARCHITECTURY_LIBRARY_KIND
) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.ARCHITECTURY_ICON
    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        return when (isCommon(classesRoots)) {
            true -> LibraryVersionProperties()
            false -> null
        }
    }

    fun isCommon(classesRoots: MutableList<VirtualFile>): Boolean {
        for (classesRoot in classesRoots) {
            if (classesRoot.name.endsWith(".jar")) {
                runCatching {
                    val jar = JarFile(classesRoot.localFile)
                    val isArchitecturyLib = jar.entries().asSequence().any {
                        it.name == "architectury.common.json"
                    }
                    if (isArchitecturyLib) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
