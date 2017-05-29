/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.filetype

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

object NbtFileType : FileType {
    override fun getDefaultExtension() = "nbt"
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getCharset(file: VirtualFile, content: ByteArray) = null
    override fun getName() = "NBT"
    override fun getDescription() = "Named Binary Tag"
    override fun isBinary() = true
    override fun isReadOnly() = false
}
