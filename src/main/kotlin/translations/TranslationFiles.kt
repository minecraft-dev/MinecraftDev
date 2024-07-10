/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.translations

import com.demonwav.mcdev.TranslationSettings
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.TranslationInverseIndex
import com.demonwav.mcdev.translations.lang.LangFile
import com.demonwav.mcdev.translations.lang.LangFileType
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.demonwav.mcdev.translations.sorting.EmptyLine
import com.demonwav.mcdev.translations.sorting.Key
import com.demonwav.mcdev.translations.sorting.Template
import com.demonwav.mcdev.translations.sorting.TemplateElement
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.mcPath
import com.demonwav.mcdev.util.mcVersion
import com.intellij.ide.DataManager
import com.intellij.json.JsonElementTypes
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.DocumentUtil
import com.intellij.util.indexing.FileBasedIndex
import java.util.Locale

object TranslationFiles {
    private val MC_1_12_2 = SemanticVersion.release(1, 12, 2)

    fun isTranslationFile(file: VirtualFile?) =
        file?.mcDomain != null && file.mcPath?.startsWith("lang/") == true &&
            file.fileType in listOf(LangFileType, JsonFileType.INSTANCE)

    fun getLocale(file: VirtualFile?) =
        file?.nameWithoutExtension?.lowercase(Locale.ENGLISH)

    fun isDefaultLocale(file: VirtualFile?) =
        file?.nameWithoutExtension?.lowercase(Locale.ENGLISH) == TranslationConstants.DEFAULT_LOCALE

    tailrec fun seekTranslation(element: PsiElement): PsiNamedElement? {
        // don't use elvis here, K2 doesn't think it's a tail recursive call if you do
        val res = toTranslation(element)?.let { element as? PsiNamedElement }
        if (res != null) {
            return res
        }
        return seekTranslation(element.parent ?: return null)
    }

    fun toTranslation(element: PsiElement): Translation? =
        if (element.containingFile?.virtualFile?.let { isTranslationFile(it) } == true) {
            when {
                element is JsonProperty && element.value is JsonStringLiteral -> Translation(
                    element.name,
                    (element.value as JsonStringLiteral).value,
                )
                element is LangEntry -> Translation(element.key, element.value)
                else -> null
            }
        } else {
            null
        }

    fun remove(element: PsiElement) {
        when (element) {
            is LangEntry -> {
                if (element.nextSibling?.node?.elementType === LangTypes.LINE_ENDING) {
                    element.nextSibling.delete()
                }
            }
            is JsonProperty -> {
                if (element.nextSibling?.node?.elementType === JsonElementTypes.COMMA) {
                    element.nextSibling.delete()
                }
            }
        }
        element.delete()
    }

    fun findTranslationKeyForText(context: PsiElement, text: String): String? {
        val module = context.findModule()
            ?: throw IllegalArgumentException("Cannot add translation for element outside of module")
        var jsonVersion = true
        if (!TranslationSettings.getInstance(context.project).isForceJsonTranslationFile) {
            val version =
                context.mcVersion ?: throw IllegalArgumentException("Cannot determine MC version for element $context")
            jsonVersion = version > MC_1_12_2
        }

        if (!jsonVersion) {
            // This feature only supports JSON translation files
            return null
        }

        val files = FileTypeIndex.getFiles(
            JsonFileType.INSTANCE,
            GlobalSearchScope.moduleScope(module),
        ).filter { getLocale(it) == TranslationConstants.DEFAULT_LOCALE }

        for (file in files) {
            val psiFile = PsiManager.getInstance(context.project).findFile(file) ?: continue
            psiFile.findKeyForTextAsJson(text)?.let { return it }
        }

        return null
    }

    fun add(context: PsiElement, key: String, text: String) {
        val module = context.findModule()
            ?: throw IllegalArgumentException("Cannot add translation for element outside of module")
        var jsonVersion = true
        if (!TranslationSettings.getInstance(context.project).isForceJsonTranslationFile) {
            val version =
                context.mcVersion ?: throw IllegalArgumentException("Cannot determine MC version for element $context")
            jsonVersion = version > MC_1_12_2
        }

        fun write(files: Iterable<VirtualFile>) {
            for (file in files) {
                val psiFile = PsiManager.getInstance(context.project).findFile(file) ?: continue
                psiFile.applyWriteAction {
                    val entries = listOf(FileEntry.Translation(key, text))
                    if (jsonVersion) {
                        this.persistAsJson(entries)
                    } else {
                        this.persistAsLang(entries)
                    }
                }
            }
        }

        val files = FileTypeIndex.getFiles(
            if (jsonVersion) JsonFileType.INSTANCE else LangFileType,
            GlobalSearchScope.moduleScope(module),
        ).filter { getLocale(it) == TranslationConstants.DEFAULT_LOCALE }
        val domains = files.asSequence().mapNotNull { it.mcDomain }.distinct().sorted().toList()
        if (domains.size > 1) {
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(domains)
                    .setTitle("Choose Resource Domain")
                    .setAdText(
                        "There are multiple resource domains with localization files, choose one for this translation.",
                    )
                    .setItemChosenCallback { domain ->
                        write(files.filter { f -> f.mcDomain == domain })
                    }
                    .setCancelOnWindowDeactivation(false)
                    .createPopup()
                    .showInBestPositionFor(it)
            }
        } else {
            write(files)
        }
    }

    fun addAll(file: PsiFile, entries: Iterable<FileEntry>) {
        when (file.fileType) {
            LangFileType -> file.persistAsLang(entries)
            JsonFileType.INSTANCE -> file.persistAsJson(entries)
            else -> throw IllegalArgumentException("Cannot add translations to file '${file.name}' of unknown type!")
        }
    }

    fun replaceAll(file: PsiFile, entries: Iterable<FileEntry>) {
        val doc = FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return
        when (file.fileType) {
            LangFileType -> {
                val content = generateLangFile(false, entries)
                doc.setText(content)
            }
            JsonFileType.INSTANCE -> {
                val rootObject = file.firstChild as? JsonObject ?: return
                val indent = rootObject.propertyList.firstOrNull()
                    ?.let { DocumentUtil.getIndent(doc, it.textOffset) } ?: "  "
                val content = generateJsonFile(false, indent, entries)
                doc.setText("{\n$content\n}")
            }
            else -> throw IllegalArgumentException(
                "Cannot replace translations in file '${file.name}' of unknown type!",
            )
        }
    }

    private fun PsiFile.persistAsLang(entries: Iterable<FileEntry>) {
        val doc = FileDocumentManager.getInstance().getDocument(this.virtualFile) ?: return
        val content = generateLangFile(
            this.lastChild != null && this.lastChild.node.elementType != LangTypes.LINE_ENDING,
            entries,
        )
        doc.insertString(this.lastChild?.textOffset ?: 0, content)
    }

    private fun generateLangFile(leadingNewLine: Boolean, entries: Iterable<FileEntry>): CharSequence {
        val result = StringBuilder()

        if (leadingNewLine) {
            result.append('\n')
        }

        for (entry in entries) {
            when (entry) {
                is FileEntry.Comment -> result.append("# ${entry.text}\n")
                is FileEntry.Translation -> result.append("${entry.key}=${entry.text}\n")
                FileEntry.EmptyLine -> result.append('\n')
                // TODO: IntelliJ shows a false error here without the `else`. The compiler doesn't care because
                //  FileEntry is a sealed class. When this bug in IntelliJ is fixed, remove this `else`.
                else -> {}
            }
        }

        return result.removeSuffix("\n")
    }

    private fun PsiFile.persistAsJson(entries: Iterable<FileEntry>) {
        val rootObject = this.firstChild as? JsonObject ?: return
        val doc = FileDocumentManager.getInstance().getDocument(this.virtualFile) ?: return
        val indent = rootObject.propertyList.firstOrNull()?.let { DocumentUtil.getIndent(doc, it.textOffset) } ?: "  "
        val content = generateJsonFile(rootObject.propertyList.isNotEmpty(), indent, entries)
        // Root object ends with brace element, so insert before that
        doc.insertString(rootObject.lastChild.prevSibling.textOffset, content)
    }

    private fun PsiFile.findKeyForTextAsJson(text: String): String? {
        val rootObject = this.firstChild as? JsonObject ?: return null
        return rootObject.propertyList.firstOrNull {
            (it.value as? JsonStringLiteral)?.value == text
        }?.name
    }

    private fun generateJsonFile(
        leadingComma: Boolean,
        indent: CharSequence,
        entries: Iterable<FileEntry>,
    ): CharSequence {
        val result = StringBuilder()

        if (leadingComma && entries.any { it is FileEntry.Translation }) {
            result.append(",\n")
        }

        for (entry in entries) {
            when (entry) {
                is FileEntry.Comment -> {}
                is FileEntry.Translation -> {
                    result.append("$indent\"${StringUtil.escapeStringCharacters(entry.key)}\": ")
                    result.append("\"${StringUtil.escapeStringCharacters(entry.text)}\",\n")
                }
                FileEntry.EmptyLine -> result.append('\n')
                // TODO: IntelliJ shows a false error here without the `else`. The compiler doesn't care because
                //  FileEntry is a sealed class. When this bug in IntelliJ is fixed, remove this `else`.
                else -> {}
            }
        }

        return result.removeSuffix("\n").removeSuffix(",")
    }

    fun buildFileEntries(project: Project, locale: String, entries: Iterable<Translation>, keepComments: Int) =
        sequence {
            for (entry in entries) {
                val langElement = TranslationInverseIndex.findElements(
                    entry.key,
                    GlobalSearchScope.allScope(project),
                    locale,
                )
                    .asSequence()
                    .mapNotNull { it as? LangEntry }
                    .firstOrNull()
                val comments: List<String> = langElement?.let { gatherLangComments(it, keepComments) } ?: emptyList()
                yieldAll(comments.asReversed().map { FileEntry.Comment(it) })
                yield(FileEntry.Translation(entry.key, entry.text))
            }
        }

    private tailrec fun gatherLangComments(
        element: PsiElement,
        maxDepth: Int,
        acc: MutableList<String> = mutableListOf(),
        depth: Int = 0,
    ): List<String> {
        if (maxDepth != 0 && depth >= maxDepth) {
            return acc
        }
        val prev = element.prevSibling ?: return acc
        if (prev.node.elementType != LangTypes.LINE_ENDING) {
            return acc
        }
        val prevLine = prev.prevSibling ?: return acc
        if (prevLine.node.elementType != LangTypes.COMMENT) {
            return acc
        }
        acc.add(prevLine.text.substring(1).trim())
        return gatherLangComments(prevLine, maxDepth, acc, depth + 1)
    }

    fun buildSortingTemplateFromDefault(context: PsiElement, domain: String? = null): Template? {
        val module = context.findModule()
            ?: throw IllegalArgumentException("Cannot add translation for element outside of module")
        var jsonVersion = true
        if (!TranslationSettings.getInstance(context.project).isForceJsonTranslationFile) {
            val version =
                context.mcVersion ?: throw IllegalArgumentException("Cannot determine MC version for element $context")
            jsonVersion = version > MC_1_12_2
        }

        val defaultTranslationFile = FileBasedIndex.getInstance()
            .getContainingFiles(
                TranslationIndex.NAME,
                TranslationConstants.DEFAULT_LOCALE,
                GlobalSearchScope.moduleScope(module),
            )
            .asSequence()
            .filter { domain == null || it.mcDomain == domain }
            .filter { (jsonVersion && it.fileType == JsonFileType.INSTANCE) || it.fileType == LangFileType }
            .firstOrNull() ?: return null
        val psi = PsiManager.getInstance(context.project).findFile(defaultTranslationFile) ?: return null

        val elements = mutableListOf<TemplateElement>()
        if (psi is LangFile) {
            for (child in psi.children) {
                when {
                    child is LangEntry ->
                        elements.add(Key(Regex.escape(child.key).toRegex()))
                    child.node.elementType == LangTypes.LINE_ENDING &&
                        child.prevSibling.node.elementType == LangTypes.LINE_ENDING ->
                        elements.add(EmptyLine)
                }
            }
        } else {
            val rootObject = psi.firstChild as? JsonObject ?: return null
            var child: PsiElement? = rootObject.firstChild
            while (child != null) {
                when (child) {
                    is JsonProperty -> elements.add(Key(Regex.escape(child.name).toRegex()))
                    is PsiWhiteSpace -> {
                        val newLines = child.text.count { it == '\n' }
                        if (newLines > 1) {
                            elements.addAll(Array(newLines - 1) { EmptyLine })
                        }
                    }
                }
                child = child.nextSibling
            }
        }
        return Template(elements)
    }

    sealed class FileEntry {
        data class Comment(val text: String) : FileEntry()

        data class Translation(val key: String, val text: String) : FileEntry()

        object EmptyLine : FileEntry()
    }
}
