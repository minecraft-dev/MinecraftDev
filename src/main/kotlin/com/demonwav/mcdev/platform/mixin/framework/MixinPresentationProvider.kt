/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.util.jar.Attributes

class MixinPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(MIXIN_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.MIXIN_ICON

    override fun detect(classesRoots: List<VirtualFile>) =
        classesRoots.asSequence()
            .map { VfsUtilCore.virtualToIoFile(it) }
            .filter { JarUtil.getJarAttribute(it, Attributes.Name("Agent-Class")) == MixinConstants.Classes.AGENT }
            .mapNotNull { JarUtil.getJarAttribute(it, Attributes.Name.IMPLEMENTATION_VERSION) }
            .map(::LibraryVersionProperties)
            .firstOrNull()
}
