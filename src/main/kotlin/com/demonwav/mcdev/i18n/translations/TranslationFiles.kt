/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.index.TranslationEntry
import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.index.TranslationInverseIndex
import com.demonwav.mcdev.i18n.lang.LangFile
import com.demonwav.mcdev.i18n.lang.LangFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
import com.demonwav.mcdev.i18n.sorting.EmptyLine
import com.demonwav.mcdev.i18n.sorting.Key
import com.demonwav.mcdev.i18n.sorting.Template
import com.demonwav.mcdev.i18n.sorting.TemplateElement
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.mcPath
import com.intellij.ide.DataManager
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.DocumentUtil
import com.intellij.util.indexing.FileBasedIndex
import java.util.Locale

object TranslationFiles {
    private val MC_1_12_2 = SemanticVersion.release(1, 12, 2)

    fun isTranslationFile(file: VirtualFile) =
        file.mcPath?.startsWith("lang/") == true && file.fileType in listOf(LangFileType, JsonFileType.INSTANCE)

    fun getLocale(file: VirtualFile) =
        file.nameWithoutExtension.toLowerCase(Locale.ROOT)

    fun add(module: Module?, key: String, text: String) {
        if (module == null) {
            return
        }

        val facet = MinecraftFacet.getInstance(module) ?: return
        val mcpModule = facet.getModuleOfType(McpModuleType) ?: return
        val version = SemanticVersion.parse(mcpModule.getSettings().minecraftVersion ?: return)
        val jsonVersion = version <= MC_1_12_2

        fun write(files: Iterable<VirtualFile>) {
            for (file in files) {
                val psiFile = PsiManager.getInstance(module.project).findFile(file) ?: continue
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

        val files = FileTypeIndex.getFiles(if (jsonVersion) JsonFileType.INSTANCE else LangFileType, GlobalSearchScope.moduleScope(module))
            .filter { getLocale(it) == I18nConstants.DEFAULT_LOCALE }
        val domains = files.asSequence().mapNotNull { it.mcDomain }.distinct().sorted().toList()
        if (domains.size > 1) {
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(domains)
                    .setTitle("Choose resource domain")
                    .setAdText("There are multiple resource domains with localization files, choose one for this translation.")
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
        when {
            file.fileType == LangFileType -> file.persistAsLang(entries)
            file.fileType == JsonFileType.INSTANCE -> file.persistAsJson(entries)
            else -> throw IllegalArgumentException("Cannot add translations to file '${file.name}' of unknown type!")
        }
    }

    fun replaceAll(file: PsiFile, entries: Iterable<FileEntry>) {
        val doc = FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return
        when {
            file.fileType == LangFileType -> {
                val content = generateLangFile(false, entries)
                doc.setText(content)
            }
            file.fileType == JsonFileType.INSTANCE -> {
                val rootObject = file.firstChild as? JsonObject ?: return
                val indent = rootObject.propertyList.firstOrNull()?.let { DocumentUtil.getIndent(doc, it.textOffset) } ?: "  "
                val content = generateJsonFile(false, indent, entries)
                doc.setText("{\n$content\n}")
            }
            else -> throw IllegalArgumentException("Cannot replace translations in file '${file.name}' of unknown type!")
        }
    }

    private fun PsiFile.persistAsLang(entries: Iterable<FileEntry>) {
        val doc = FileDocumentManager.getInstance().getDocument(this.virtualFile) ?: return
        val content = generateLangFile(this.lastChild != null && this.lastChild.node.elementType != LangTypes.LINE_ENDING, entries)
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

    private fun generateJsonFile(leadingComma: Boolean, indent: CharSequence, entries: Iterable<FileEntry>): CharSequence {
        val result = StringBuilder()

        if (leadingComma && entries.any { it is FileEntry.Translation }) {
            result.append(",\n")
        }

        for (entry in entries) {
            when (entry) {
                is FileEntry.Comment -> {
                }
                is FileEntry.Translation -> {
                    result.append("$indent\"${StringUtil.escapeStringCharacters(entry.key)}\": ")
                    result.append("\"${StringUtil.escapeStringCharacters(entry.text)}\",\n")
                }
                FileEntry.EmptyLine -> result.append('\n')
            }
        }

        return result.removeSuffix("\n").removeSuffix(",")
    }

    fun buildFileEntries(project: Project, locale: String, entries: Sequence<TranslationEntry>, keepComments: Int) =
        sequence {
            for (entry in entries) {
                val langElement = TranslationInverseIndex.findElements(entry.key, GlobalSearchScope.allScope(project), locale)
                    .asSequence()
                    .mapNotNull { it as? LangEntry }
                    .firstOrNull()
                val comments = langElement?.let { gatherLangComments(it, keepComments) } ?: emptyList()
                yieldAll(comments.asReversed().map { FileEntry.Comment(it) })
                yield(FileEntry.Translation(entry.key, entry.text))
            }
        }

    private tailrec fun gatherLangComments(element: PsiElement, maxDepth: Int, acc: MutableList<String> = mutableListOf(), depth: Int = 0): List<String> {
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

    fun buildSortingTemplateFromDefault(module: Module, domain: String? = null): Template? {
        val facet = MinecraftFacet.getInstance(module) ?: return null
        val mcpModule = facet.getModuleOfType(McpModuleType) ?: return null
        val version = SemanticVersion.parse(mcpModule.getSettings().minecraftVersion ?: return null)
        val jsonVersion = version <= MC_1_12_2

        val defaultTranslationFile = FileBasedIndex.getInstance().getContainingFiles(
            TranslationIndex.NAME, I18nConstants.DEFAULT_LOCALE, GlobalSearchScope.projectScope(module.project)
        ).asSequence()
            .filter { domain == null || it.mcDomain == domain }
            .filter { (jsonVersion && it.fileType == JsonFileType.INSTANCE) || it.fileType == LangFileType }
            .firstOrNull() ?: return null
        val psi = PsiManager.getInstance(module.project).findFile(defaultTranslationFile) ?: return null

        val elements = mutableListOf<TemplateElement>()
        if (psi is LangFile) {
            for (child in psi.children) {
                when {
                    child is LangEntry ->
                        elements.add(Key(Regex.escape(child.key).toRegex()))
                    child.node.elementType == LangTypes.LINE_ENDING && child.prevSibling.node.elementType == LangTypes.LINE_ENDING ->
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
