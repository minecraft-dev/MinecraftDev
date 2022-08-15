/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.util.io.JarUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.util.jar.JarFile

class VelocityPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(VELOCITY_LIBRARY_KIND) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.VELOCITY_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            runCatching {
                if (JarUtil.containsClass(classesRoot.localFile, "com.velocitypowered.api.proxy.ProxyServer")) {
                    return LibraryVersionProperties()
                }

                // Velocity API jar has no Manifest entries, so we search for their annotation processor instead
                val registeredAPs = JarFile(classesRoot.localFile).use { jar ->
                    val aps = jar.getEntry("META-INF/services/javax.annotation.processing.Processor")
                        ?: return@use null
                    jar.getInputStream(aps).bufferedReader().use(BufferedReader::readLines)
                } ?: return@runCatching

                if (registeredAPs.contains("com.velocitypowered.api.plugin.ap.PluginAnnotationProcessor")) {
                    return LibraryVersionProperties()
                }
            }
        }
        return null
    }
}
