/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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

// Technically resource domains are much more restricted ([a-z0-9_-]+) in modern versions, but we want to support as much as possible
private val DOMAIN_PATTERN = Regex("^.*?/assets/([^/]+)/lang.*?$")

val VirtualFile.mcDomain: String?
    get() = DOMAIN_PATTERN.matchEntire(this.path)?.groupValues?.get(1)

operator fun Manifest.get(attribute: String): String? = mainAttributes.getValue(attribute)
operator fun Manifest.get(attribute: Attributes.Name): String? = mainAttributes.getValue(attribute)

fun VirtualFile.refreshFs(): VirtualFile {
    return this.parent.findOrCreateChildData(this, this.name)
}
