/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

class McpPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(MCP_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.MCP_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = VfsUtilCore.virtualToIoFile(classesRoot)

            if (JarUtil.containsClass(file, McpConstants.MINECRAFT_SERVER) && !JarUtil.containsClass(file, ForgeConstants.MOD_ANNOTATION)) {
                return LibraryVersionProperties("unknown") // TODO: Somehow get version?
            }
        }
        return null
    }
}
