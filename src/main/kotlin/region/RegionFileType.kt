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

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

object RegionFileType : FileType {
    override fun getDefaultExtension() = "mca"
    override fun getIcon() = AllIcons.FileTypes.Archive
    override fun getCharset(file: VirtualFile, content: ByteArray) = null
    override fun getName() = "MCA"
    override fun getDescription() = MCDevBundle("region.file_type.description")
    override fun isBinary() = true
    override fun isReadOnly() = true
}
