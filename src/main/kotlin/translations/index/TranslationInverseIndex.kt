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

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.VoidDataExternalizer

class TranslationInverseIndex : FileBasedIndexExtension<String, Void>() {
    override fun getName() = NAME

    override fun getVersion() = 1

    override fun dependsOnFileContent() = true

    override fun getValueExternalizer(): DataExternalizer<Void> = VoidDataExternalizer.INSTANCE

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = Indexer

    override fun getInputFilter(): FileBasedIndex.InputFilter = TranslationInputFilter

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    companion object {
        val NAME = ID.create<String, Void>("TranslationInverseIndex")

        fun findElements(key: String, filter: GlobalSearchScope, locale: String? = null): List<PsiElement> {
            val project = filter.project ?: return emptyList()
            if (DumbService.isDumb(project)) {
                return emptyList()
            }

            return FileBasedIndex.getInstance().getContainingFiles(NAME, key, filter)
                .filter {
                    locale == null || TranslationFiles.getLocale(it) == locale
                }
                .flatMap {
                    TranslationProvider.INSTANCES[it.fileType]?.findElements(project, it, key) ?: emptyList()
                }
        }
    }

    private object Indexer : DataIndexer<String, Void, FileContent> {
        override fun map(inputData: FileContent): MutableMap<String, Void?> {
            val domain = inputData.file.mcDomain ?: return mutableMapOf()
            val entry = TranslationProvider.INSTANCES[inputData.fileType]?.map(domain, inputData)
                ?: return mutableMapOf()
            return entry.translations.associateTo(mutableMapOf()) { it.key to null }
        }
    }
}
