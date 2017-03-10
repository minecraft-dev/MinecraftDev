/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

class SpigotPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(SPIGOT_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.SPIGOT_ICON

    override fun detect(classesRoots: List<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            val file = VfsUtilCore.virtualToIoFile(classesRoot)
            val properties = JarUtil.loadProperties(file, "META-INF/maven/org.spigotmc/spigot-api/pom.properties") ?: continue

            if (properties["groupId"] != "org.spigotmc" || properties["artifactId"] != "spigot-api") {
                continue
            }

            val version = properties["version"] as? String ?: continue
            return LibraryVersionProperties(version)
        }
        return null
    }
}
