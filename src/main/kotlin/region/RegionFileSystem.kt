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

package com.demonwav.mcdev.region

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.openapi.vfs.newvfs.VfsImplUtil
import com.intellij.util.io.URLUtil

private const val PROTOCOL = "mcdev-region"

class RegionFileSystem : ArchiveFileSystem() {
    @Suppress("CompanionObjectInExtension") // False-positive: this is a getter, not a field
    companion object {
        @JvmStatic
        val INSTANCE get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as RegionFileSystem
    }

    override fun getProtocol() = PROTOCOL
    override fun isReadOnly() = true
    override fun isCorrectFileType(local: VirtualFile) = local.fileType is RegionFileType

    override fun extractRootPath(path: String) = extractLocalPath(path) + URLUtil.JAR_SEPARATOR
    override fun extractLocalPath(archivePath: String) = archivePath.substringBeforeLast(URLUtil.JAR_SEPARATOR)
    override fun composeRootPath(localPath: String) = "$localPath${URLUtil.JAR_SEPARATOR}"

    override fun findFileByPath(path: String): VirtualFile? = VfsImplUtil.findFileByPath(this, path)
    override fun refresh(asynchronous: Boolean) = VfsImplUtil.refresh(this, asynchronous)
    override fun refreshAndFindFileByPath(path: String) = VfsImplUtil.refreshAndFindFileByPath(this, path)
    override fun findFileByPathIfCached(path: String) = VfsImplUtil.findFileByPathIfCached(this, path)
    override fun getHandler(entryFile: VirtualFile) = VfsImplUtil.getHandler(this, entryFile, ::RegionArchiveHandler)
}
