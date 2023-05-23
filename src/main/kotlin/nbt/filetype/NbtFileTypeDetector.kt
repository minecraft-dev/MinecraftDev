/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.nbt.filetype

import com.demonwav.mcdev.nbt.Nbt
import com.demonwav.mcdev.nbt.NbtFileParseTimeoutException
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile

class NbtFileTypeDetector : FileTypeRegistry.FileTypeDetector {
    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        return try {
            // 20 ms is plenty of time to parse most files
            // Won't parse very large files, but if we fail on timeout then those files probably are NBT anyways
            Nbt.buildTagTree(file.inputStream, 20)
            NbtFileType
        } catch (e: Throwable) {
            if (e is NbtFileParseTimeoutException) {
                // If a timeout occurred then no file structure errors were detected in the parse time, so we can
                // probably assume it's a (very big) NBT file
                NbtFileType
            } else {
                null
            }
        }
    }
}
