/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.index

import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.mcPath
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.PsiDependentIndex
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class TranslationIndex : FileBasedIndexExtension<String, TranslationIndexEntry>(), PsiDependentIndex {
    override fun getName() = NAME

    override fun getVersion() = 5

    override fun dependsOnFileContent() = true

    override fun getValueExternalizer(): DataExternalizer<TranslationIndexEntry> = ValueExternalizer

    override fun getIndexer(): DataIndexer<String, TranslationIndexEntry, FileContent> = Indexer

    override fun getInputFilter(): FileBasedIndex.InputFilter = TranslationInputFilter

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    companion object {
        val NAME = ID.create<String, TranslationIndexEntry>("TranslationIndex")
    }

    private object ValueExternalizer : DataExternalizer<TranslationIndexEntry> {
        override fun save(output: DataOutput, value: TranslationIndexEntry) {
            output.writeUTF(value.sourceDomain)
            val translations = value.translations
            output.writeInt(translations.size)
            for ((key, translation) in translations) {
                output.writeUTF(key)
                output.writeUTF(translation)
            }
        }

        override fun read(input: DataInput): TranslationIndexEntry {
            val sourceDomain = input.readUTF()
            val translations = (0 until input.readInt()).map { TranslationEntry(input.readUTF(), input.readUTF()) }
            return TranslationIndexEntry(sourceDomain, translations)
        }
    }

    private object Indexer : DataIndexer<String, TranslationIndexEntry, FileContent> {
        override fun map(inputData: FileContent): MutableMap<String, TranslationIndexEntry> {
            val domain = inputData.file.mcDomain ?: return mutableMapOf()
            val entry = TranslationProvider.INSTANCES[inputData.fileType]?.map(domain, inputData) ?: return mutableMapOf()
            return mutableMapOf(inputData.file.nameWithoutExtension to entry)
        }
    }
}
