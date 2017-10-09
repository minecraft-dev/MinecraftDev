/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

val VirtualFile.localFile: File
    get() = VfsUtilCore.virtualToIoFile(this)

val VirtualFile.manifest: Manifest?
    get() = try {
        JarFile(localFile).use { it.manifest }
    } catch (e: IOException) {
        null
    }

operator fun Manifest.get(attribute: String): String? = mainAttributes.getValue(attribute)
operator fun Manifest.get(attribute: Attributes.Name): String? = mainAttributes.getValue(attribute)
