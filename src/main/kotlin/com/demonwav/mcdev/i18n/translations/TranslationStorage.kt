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
import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.index.TranslationEntry
import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.mcDomain
import com.intellij.ide.DataManager
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.Locale

object TranslationStorage {
    private val MC_1_12_2 = SemanticVersion.release(1, 12, 2)

    fun persist(module: Module?, key: String, text: String) {
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
                    persist(this, key, text, jsonVersion)
                }
            }
        }

        val jsonFiles = FileTypeIndex.getFiles(JsonFileType.INSTANCE, GlobalSearchScope.moduleScope(module))
        val langFiles = FileTypeIndex.getFiles(I18nFileType, GlobalSearchScope.moduleScope(module))
        val files = (jsonFiles + langFiles).filter { it.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE }
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
                    .createPopup()
                    .showInBestPositionFor(it)
            }
        } else {
            write(files)
        }
    }

    fun addAll(file: PsiFile, entries: Iterable<TranslationEntry>) {
        val facet = MinecraftFacet.getInstance(file.findModule() ?: return) ?: return
        val mcpModule = facet.getModuleOfType(McpModuleType) ?: return
        val version = SemanticVersion.parse(mcpModule.getSettings().minecraftVersion ?: return)
        val jsonVersion = version <= MC_1_12_2

        for (entry in entries) {
            persist(file, entry.key, entry.text, jsonVersion)
        }
    }

    private fun persist(file: PsiFile, key: String, text: String, jsonVersion: Boolean) {
        if (jsonVersion) {
            file.persistAsJson(key, text)
        } else {
            file.persistAsLang(key, text)
        }
    }

    private fun PsiFile.persistAsLang(key: String, text: String) {
        if (this is I18nFile) {
            add(I18nElementFactory.createLineEnding(project))
            add(I18nElementFactory.createEntry(project, key, text))
        }
    }

    private fun PsiFile.persistAsJson(key: String, text: String) {
        val jsonGenerator = JsonElementGenerator(this.project)
        if (this is JsonFile) {
            val value = jsonGenerator.createStringLiteral(text)
            val rootObject = this.firstChild as? JsonObject ?: return
            rootObject.addBefore(jsonGenerator.createComma(), rootObject.lastChild)
            rootObject.addBefore(jsonGenerator.createProperty(key, value.text), rootObject.lastChild)
        }
    }
}
