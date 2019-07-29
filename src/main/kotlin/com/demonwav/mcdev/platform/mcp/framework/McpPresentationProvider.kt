/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile

class McpPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(MCP_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.MCP_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = classesRoot.localFile

            if (JarUtil.containsClass(file, McpConstants.MINECRAFT_SERVER)) {
                return LibraryVersionProperties()
            }
        }
        return null
    }
}
