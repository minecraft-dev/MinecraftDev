/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.filetype

import com.demonwav.mcdev.nbt.Nbt
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile

class NbtFileTypeDetector : FileTypeRegistry.FileTypeDetector {
    override fun getVersion() = 1

    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? {
        return try {
            Nbt.buildTagTree(file.inputStream, 100)
            NbtFileType
        } catch (e: Exception) {
            null
        }
    }
}
