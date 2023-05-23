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

package com.demonwav.mcdev.translations.index

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.lang.LangFile
import com.demonwav.mcdev.translations.lang.LangFileType
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.util.childrenOfType
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.indexing.FileContent

interface TranslationProvider {
    fun map(domain: String, input: FileContent): TranslationIndexEntry?

    fun findElements(project: Project, file: VirtualFile, key: String): List<PsiElement>

    companion object {
        // Use name, using FileType as map keys can leak and cause problems with plugin unloading
        // name is unique among all file types
        val INSTANCES = mapOf(
            JsonFileType.INSTANCE.name to JsonTranslationProvider,
            LangFileType.name to LangTranslationProvider,
        )
    }
}

object JsonTranslationProvider : TranslationProvider {
    override fun map(domain: String, input: FileContent): TranslationIndexEntry? {
        val json = try {
            JsonParser.parseString(input.contentAsText.toString())
        } catch (_: JsonSyntaxException) {
            return null
        }
        if (!json.isJsonObject) {
            return null
        }
        val obj = json.asJsonObject
        val translations = obj.entrySet().asSequence()
            .filter { it.value.isJsonPrimitive }
            .map { Translation(it.key, it.value.asString) }
            .toList()
        return TranslationIndexEntry(domain, translations)
    }

    override fun findElements(project: Project, file: VirtualFile, key: String): List<JsonProperty> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(
            psiFile,
            Key<CachedValue<List<JsonProperty>>>("translation_lookup.$key"),
        ) {
            val value = psiFile.topLevelValue as? JsonObject
            CachedValueProvider.Result.create(
                value?.propertyList?.filter { it.name == key } ?: emptyList(),
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }
    }
}

object LangTranslationProvider : TranslationProvider {
    override fun map(domain: String, input: FileContent): TranslationIndexEntry {
        val translations = input.contentAsText
            .lineSequence()
            .filter { !it.startsWith("#") && it.isNotEmpty() }
            .mapTo(mutableListOf()) {
                val entry = it.split("=")
                Translation(entry.first(), entry.getOrNull(1) ?: "")
            }
        return TranslationIndexEntry(domain, translations)
    }

    override fun findElements(project: Project, file: VirtualFile, key: String): List<LangEntry> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? LangFile ?: return emptyList()
        return CachedValuesManager.getCachedValue(
            psiFile,
            Key("translation_lookup.$key"),
        ) {
            CachedValueProvider.Result.create(
                psiFile.childrenOfType<LangEntry>().filter { it.key == key },
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }
    }
}
