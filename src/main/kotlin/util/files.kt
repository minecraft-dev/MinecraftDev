/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.RefreshQueue
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

fun VirtualFile.refreshSync(modalityState: ModalityState): VirtualFile? {
    RefreshQueue.getInstance().refresh(false, this.isDirectory, null, modalityState, this)
    return this.parent?.findOrCreateChildData(this, this.name)
}
