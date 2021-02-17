/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.util.localFile
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.IOException
import java.util.jar.JarFile

class VelocityPresentationProvider : LibraryPresentationProvider<LibraryVersionProperties>(VELOCITY_LIBRARY_KIND) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.VELOCITY_ICON

    override fun detect(classesRoots: MutableList<VirtualFile>): LibraryVersionProperties? {
        for (classesRoot in classesRoots) {
            try {
                // Velocity API jar has no Manifest entries, so we search for their annotation processor instead
                val registeredAPs = JarFile(classesRoot.localFile).use { jar ->
                    val aps = jar.getEntry("META-INF/services/javax.annotation.processing.Processor")
                        ?: return@use null
                    jar.getInputStream(aps).bufferedReader().use(BufferedReader::readLines)
                } ?: continue

                if (registeredAPs.contains("com.velocitypowered.api.plugin.ap.PluginAnnotationProcessor")) {
                    return LibraryVersionProperties()
                }
            } catch (ignored: IOException) {
            }
        }
        return null
    }
}
