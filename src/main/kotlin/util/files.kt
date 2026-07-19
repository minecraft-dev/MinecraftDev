/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.TransactionGuard
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

val VirtualFile.localFile: File
    get() = VfsUtilCore.virtualToIoFile(this)

val Path.virtualFile: VirtualFile?
    get() = LocalFileSystem.getInstance().refreshAndFindFileByPath(this.toAbsolutePath().toString())

val Path.virtualFileOrError: VirtualFile
    get() = virtualFile ?: throw IllegalStateException("Failed to find file: $this")

val VirtualFile.manifest: Manifest?
    get() = try {
        JarFile(localFile).use { it.manifest }
    } catch (_: IOException) {
        null
    }

val VirtualFile.mcDomain: String?
    get() = mcDomainAndPath?.first
val VirtualFile.mcPath: String?
    get() = mcDomainAndPath?.second
val VirtualFile.mcDomainAndPath: Pair<String, String>?
    get() {
        var domain: String? = null
        val path = mutableListOf<String>()
        var vf: VirtualFile? = this
        while (vf != null) {
            val name = vf.name
            if (name == "assets" || name == "data") {
                break
            }
            domain?.let(path::add)
            domain = name
            vf = vf.parent
        }
        if (vf == null || domain == null) {
            // if vf is null, we never found "assets" or "data", if domain is null our file path was too short.
            return null
        }
        path.reverse()
        return domain to path.joinToString("/")
    }

operator fun Manifest.get(attribute: String): String? = mainAttributes.getValue(attribute)
operator fun Manifest.get(attribute: Attributes.Name): String? = mainAttributes.getValue(attribute)

suspend fun VirtualFile.refreshSync(modalityState: ModalityState): VirtualFile? {
    val file = this
    return withContext(Dispatchers.EDT) {
        val state = if (TransactionGuard.getInstance().isWriteSafeModality(modalityState)) {
            modalityState
        } else {
            ModalityState.current()
        }

        return@withContext suspendCancellableCoroutine { continuation ->
            try {
                RefreshQueue.getInstance().refresh(true, file.isDirectory, {
                    continuation.resume(file.parent?.findChild(file.name))
                }, state, file)
            } catch (t: Throwable) {
                continuation.cancel(t)
            }
        }
    }
}
