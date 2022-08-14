/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.JarFile

class FabricPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(FABRIC_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.FABRIC_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            if (classesRoot.name.endsWith(".jar")) {
                runCatching {
                    val jar = JarFile(classesRoot.localFile)
                    val isFabricLib = jar.entries().asSequence().any {
                        it.name == "net/fabricmc/loader/api/FabricLoader.class"
                    }
                    if (isFabricLib) {
                        return LibraryVersionProperties()
                    }
                }
            }
        }
        return null
    }
}
