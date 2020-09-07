/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.index

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.intellij.util.indexing.FileBasedIndex

object TranslationInputFilter : FileBasedIndex.FileTypeSpecificInputFilter {
    override fun registerFileTypesUsedForIndexing(fileTypeSink: Consumer<in FileType>) {
        for (fileType in TranslationProvider.INSTANCES.keys) {
            fileTypeSink.consume(fileType)
        }
    }

    override fun acceptInput(file: VirtualFile): Boolean {
        return TranslationFiles.isTranslationFile(file)
    }
}
