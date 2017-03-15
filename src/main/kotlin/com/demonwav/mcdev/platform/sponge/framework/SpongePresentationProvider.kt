/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes
import java.util.jar.JarFile

class SpongePresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(SPONGE_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.SPONGE_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        loop@ for (classesRoot in classesRoots) {
            val file = VfsUtilCore.virtualToIoFile(classesRoot)
            val manifest = JarFile(file).manifest ?: continue

            val mainAttributes = manifest.mainAttributes

            val versionAttribute = when ("SpongeAPI") {
                mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE) -> Attributes.Name.IMPLEMENTATION_VERSION
                mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE) -> Attributes.Name.SPECIFICATION_VERSION
                else -> continue@loop
            }

            val version = mainAttributes.getValue(versionAttribute) ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
