/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.get
import com.demonwav.mcdev.util.manifest
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION

class MixinPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(MIXIN_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.MIXIN_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val manifest = classesRoot.manifest ?: continue
            if (manifest["Agent-Class"] != MixinConstants.Classes.MIXIN_AGENT) {
                continue
            }

            val version = manifest[IMPLEMENTATION_VERSION] ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
