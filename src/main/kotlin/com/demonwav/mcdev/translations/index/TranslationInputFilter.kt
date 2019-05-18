/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.index

import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.mcPath
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.intellij.util.indexing.FileBasedIndex

object TranslationInputFilter : FileBasedIndex.FileTypeSpecificInputFilter {
    override fun registerFileTypesUsedForIndexing(fileTypeSink: Consumer<FileType>) {
        for (fileType in TranslationProvider.INSTANCES.keys) {
            fileTypeSink.consume(fileType)
        }
    }

    override fun acceptInput(file: VirtualFile): Boolean {
        return file.mcDomain != null && file.mcPath?.startsWith("lang/") == true
    }
}
