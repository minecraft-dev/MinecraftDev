/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

val VirtualFile.localFile: File
    get() = VfsUtilCore.virtualToIoFile(this)

val Path.virtualFile: VirtualFile?
    get() = LocalFileSystem.getInstance().refreshAndFindFileByPath(this.toAbsolutePath().toString())

val Path.virtualFileOrError: VirtualFile
    get() = virtualFile ?: throw IllegalStateException("Failed to find file: $this")

val VirtualFile.manifest: Manifest?
    get() = try {
        JarFile(localFile).use { it.manifest }
    } catch (e: IOException) {
        null
    }

// Technically resource domains are much more restricted ([a-z0-9_-]+) in modern versions, but we want to support as much as possible
private val RESOURCE_LOCATION_PATTERN = Regex("^.*?/(assets|data)/([^/]+)/(.*?)$")

val VirtualFile.mcDomain: String?
    get() = RESOURCE_LOCATION_PATTERN.matchEntire(this.path)?.groupValues?.get(2)
val VirtualFile.mcPath: String?
    get() = RESOURCE_LOCATION_PATTERN.matchEntire(this.path)?.groupValues?.get(3)

operator fun Manifest.get(attribute: String): String? = mainAttributes.getValue(attribute)
operator fun Manifest.get(attribute: Attributes.Name): String? = mainAttributes.getValue(attribute)

fun VirtualFile.refreshFs(): VirtualFile {
    return this.parent.findOrCreateChildData(this, this.name)
}
