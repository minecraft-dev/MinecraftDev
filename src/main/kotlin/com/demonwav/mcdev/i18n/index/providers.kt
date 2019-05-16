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

import com.demonwav.mcdev.i18n.lang.LangFile
import com.demonwav.mcdev.i18n.lang.LangFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.google.gson.JsonParser
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.indexing.FileContent
import org.jetbrains.plugins.groovy.lang.psi.util.childrenOfType

interface TranslationProvider {
    fun map(domain: String, input: FileContent): TranslationIndexEntry?

    fun findElements(project: Project, file: VirtualFile, key: String): List<PsiElement>

    companion object {
        val INSTANCES = mapOf(
            LangFileType to LangTranslationProvider,
            JsonFileType.INSTANCE to JsonTranslationProvider
        )
    }
}

object LangTranslationProvider : TranslationProvider {
    override fun map(domain: String, input: FileContent): TranslationIndexEntry? {
        val translations = input.contentAsText.lineSequence().filter { !it.startsWith("#") && it.isNotEmpty() }.mapTo(mutableListOf()) {
            val entry = it.split("=")
            TranslationEntry(entry.first(), entry.getOrNull(1) ?: "")
        }
        return TranslationIndexEntry(domain, translations)
    }

    override fun findElements(project: Project, file: VirtualFile, key: String): List<LangEntry> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? LangFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(
            psiFile,
            Key("translation_lookup.$key")
        ) {
            CachedValueProvider.Result.create(
                psiFile.childrenOfType<LangEntry>().filter { it.key == key },
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}

object JsonTranslationProvider : TranslationProvider {
    override fun map(domain: String, input: FileContent): TranslationIndexEntry? {
        val json = JsonParser().parse(input.contentAsText.toString())
        if (!json.isJsonObject) {
            return null
        }
        val obj = json.asJsonObject
        return TranslationIndexEntry(domain, obj.entrySet().map { TranslationEntry(it.key, it.value.asString) })
    }

    override fun findElements(project: Project, file: VirtualFile, key: String): List<JsonProperty> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(
            psiFile,
            Key("translation_lookup.$key")
        ) {
            val value = psiFile.topLevelValue as? JsonObject
            CachedValueProvider.Result.create(
                value?.propertyList?.filter { it.name == key } ?: emptyList(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}
