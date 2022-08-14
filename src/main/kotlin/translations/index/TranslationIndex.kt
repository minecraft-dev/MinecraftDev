/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.index

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class TranslationIndex : FileBasedIndexExtension<String, TranslationIndexEntry>() {
    override fun getName() = NAME

    override fun getVersion() = 6

    override fun dependsOnFileContent() = true

    override fun getValueExternalizer(): DataExternalizer<TranslationIndexEntry> = ValueExternalizer

    override fun getIndexer(): DataIndexer<String, TranslationIndexEntry, FileContent> = Indexer

    override fun getInputFilter(): FileBasedIndex.InputFilter = TranslationInputFilter

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    companion object {
        val NAME = ID.create<String, TranslationIndexEntry>("TranslationIndex")

        fun getAllDefaultTranslations(project: Project, domain: String? = null) =
            getAllDefaultEntries(project, domain).flatten()

        fun getProjectDefaultTranslations(project: Project, domain: String? = null) =
            getProjectDefaultEntries(project, domain).flatten()

        fun getTranslations(project: Project, file: VirtualFile): Sequence<Translation> {
            return getEntries(
                GlobalSearchScope.fileScope(project, file),
                TranslationFiles.getLocale(file) ?: return emptySequence(),
                file.mcDomain
            ).flatten()
        }

        fun getTranslations(file: PsiFile): Sequence<Translation> {
            val virtualFile = file.virtualFile
            return getEntries(
                GlobalSearchScope.fileScope(file),
                TranslationFiles.getLocale(virtualFile) ?: return emptySequence(),
                virtualFile.mcDomain
            ).flatten()
        }

        fun getAllDefaultEntries(project: Project, domain: String? = null) =
            getEntries(GlobalSearchScope.allScope(project), TranslationConstants.DEFAULT_LOCALE, domain)

        fun getProjectDefaultEntries(project: Project, domain: String? = null) =
            getEntries(GlobalSearchScope.projectScope(project), TranslationConstants.DEFAULT_LOCALE, domain)

        fun getEntries(scope: GlobalSearchScope, locale: String, domain: String? = null) =
            FileBasedIndex.getInstance().getValues(
                TranslationIndex.NAME,
                locale,
                scope
            ).asSequence()
                .filter { domain == null || it.sourceDomain == domain }

        private fun Sequence<TranslationIndexEntry>.flatten() = this.flatMap { it.translations.asSequence() }
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
            val translations = (0 until input.readInt()).map { Translation(input.readUTF(), input.readUTF()) }
            return TranslationIndexEntry(sourceDomain, translations)
        }
    }

    private object Indexer : DataIndexer<String, TranslationIndexEntry, FileContent> {
        override fun map(inputData: FileContent): MutableMap<String, TranslationIndexEntry> {
            val domain = inputData.file.mcDomain ?: return mutableMapOf()
            val entry = TranslationProvider.INSTANCES[inputData.fileType]?.map(domain, inputData)
                ?: return mutableMapOf()
            val locale = TranslationFiles.getLocale(inputData.file) ?: return mutableMapOf()
            return mutableMapOf(locale to entry)
        }
    }
}
