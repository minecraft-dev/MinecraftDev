/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.filetype

import com.demonwav.mcdev.nbt.Nbt
import com.demonwav.mcdev.nbt.NbtFileParseTimeoutException
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile

class NbtFileTypeDetector : FileTypeRegistry.FileTypeDetector {
    override fun getVersion() = 1

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
