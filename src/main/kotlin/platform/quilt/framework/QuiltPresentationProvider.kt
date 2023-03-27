/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.JarFile

class QuiltPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(QUILT_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.QUILT_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            if (classesRoot.name.endsWith(".jar")) {
                runCatching {
                    val jar = JarFile(classesRoot.localFile)
                    val isQuiltLib = jar.entries().asSequence().any {
                        it.name == "org/quiltmc/loader/api/QuiltLoader.class"
                    }
                    if (isQuiltLib) {
                        return LibraryVersionProperties()
                    }
                }
            }
        }
        return null
    }
}
