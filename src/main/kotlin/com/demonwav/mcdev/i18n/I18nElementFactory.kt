/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.demonwav.mcdev.util.applyWriteAction
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBList
import com.intellij.util.Consumer
import java.util.Locale

object I18nElementFactory {
    val DOMAIN_PATTERN = Regex("^.*?/assets/(.*?)/lang.*?\$")

    fun getResourceDomain(file: VirtualFile) =
        DOMAIN_PATTERN.matchEntire(file.path)?.groupValues?.get(1)

    fun addTranslation(module: Module?, name: String, value: String?) {
        if (module == null || value == null) {
            return
        }
        fun write(files: Iterable<VirtualFile>) {
            for (file in files) {
                val simpleFile = PsiManager.getInstance(module.project).findFile(file)
                if (simpleFile is I18nFile) {
                    simpleFile.applyWriteAction {
                        add(createLineEnding(project))
                        add(createProperty(project, name, value))
                    }
                }
            }
        }

        val files = FileTypeIndex.getFiles(I18nFileType, GlobalSearchScope.moduleScope(module))
        if (files.count { it.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE } > 1) {
            val choices = files.mapNotNull(this::getResourceDomain).distinct().sorted()
            val swingList = JBList(choices)
            DataManager.getInstance().dataContextFromFocus.doWhenDone(Consumer<DataContext> {
                JBPopupFactory.getInstance()
                    .createListPopupBuilder(swingList)
                    .setTitle("Choose resource domain")
                    .setAdText("There are multiple resource domains with localization files, choose one for this translation.")
                    .setItemChoosenCallback {
                        swingList.selectedValue?.let {
                            val validPattern = Regex("^.*?/assets/${Regex.escape(it)}/lang.*?\$")
                            write(files.filter { validPattern.matches(it.path) })
                        }
                    }
                    .createPopup()
                    .showInBestPositionFor(it)
            })
        } else {
            write(files)
        }
    }

    fun createFile(project: Project, text: String): I18nFile {
        return PsiFileFactory.getInstance(project).createFileFromText("name", I18nFileType, text) as I18nFile
    }

    fun createComment(project: Project, text: String): PsiElement {
        val file = createFile(project, "# $text")
        return file.firstChild
    }

    fun createProperty(project: Project, key: String, value: String = ""): I18nProperty {
        val file = createFile(project, "$key=$value")
        return file.firstChild as I18nProperty
    }

    fun createLineEnding(project: Project): PsiElement {
        val file = createFile(project, "\n")
        return file.firstChild
    }
}
