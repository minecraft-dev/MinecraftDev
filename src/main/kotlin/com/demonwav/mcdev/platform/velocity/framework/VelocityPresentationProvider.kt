/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION

class VelocityPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(VELOCITY_LIBRARY_KIND) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.VELOCITY_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue

            val version = manifest[IMPLEMENTATION_VERSION] ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
