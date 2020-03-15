/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION
import java.util.jar.Attributes.Name.SPECIFICATION_TITLE
import java.util.jar.Attributes.Name.SPECIFICATION_VERSION

class SpongePresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(SPONGE_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.SPONGE_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        loop@ for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            val versionAttribute = when ("SpongeAPI") {
                manifest[IMPLEMENTATION_TITLE] -> IMPLEMENTATION_VERSION
                manifest[SPECIFICATION_TITLE] -> SPECIFICATION_VERSION
                else -> continue@loop
            }

            val version = manifest[versionAttribute] ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
