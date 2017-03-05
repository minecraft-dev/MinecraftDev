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
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

class SpigotPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(SPIGOT_LIBRARY_KIND) {

    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.SPIGOT_ICON

    override fun detect(classesRoots: List<VirtualFile>) =
        classesRoots.asSequence()
            .map { VfsUtilCore.virtualToIoFile(it) }
            .mapNotNull { JarUtil.loadProperties(it, "META-INF/maven/org.spigotmc/spigot-api/pom.properties") }
            .filter { it["groupId"] == "org.spigotmc" && it["artifactId"] == "spigot-api" }
            .mapNotNull { it["version"] as? String }
            .map(::LibraryVersionProperties)
            .firstOrNull()
}
