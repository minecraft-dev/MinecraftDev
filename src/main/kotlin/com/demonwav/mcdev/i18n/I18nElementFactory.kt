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
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import javax.swing.JList

object I18nElementFactory {
    val DOMAIN_PATTERN = Regex("^.*?/assets/(.*?)/lang.*?\$")

    fun getResourceDomain(file: VirtualFile) =
        DOMAIN_PATTERN.matchEntire(file.path)?.groupValues?.get(1)

    fun addTranslation(module: Module?, name: String, value: String?) {
        if (module == null || value == null)
            return
        fun write(files: Iterable<VirtualFile>) {
            for (file in files) {
                val simpleFile = PsiManager.getInstance(module.project).findFile(file)
                if (simpleFile is I18nFile) {
                    object : WriteCommandAction.Simple<Unit>(module.project, simpleFile) {
                        @Throws(Throwable::class)
                        override fun run() {
                            simpleFile.add(createLineEnding(module.project))
                            simpleFile.add(createProperty(module.project, name, value))
                        }
                    }.execute()
                    PsiDocumentManager.getInstance(module.project).doPostponedOperationsAndUnblockDocument(FileDocumentManager.getInstance().getDocument(file)!!)
                }
            }
        }

        val files = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, I18nFileType, GlobalSearchScope.moduleScope(module))
        val fileNames = files.map { it.nameWithoutExtension.toLowerCase() }.filter { it == I18nConstants.DEFAULT_LOCALE }
        if (fileNames.size > 1) {
            val choices = files.mapNotNull(this::getResourceDomain).distinct().sortedBy { it }
            val swingList = JList(choices.toTypedArray())
            JBPopupFactory.getInstance()
                .createListPopupBuilder(swingList)
                .setTitle("Choose resource domain")
                .setAdText("There are multiple resource domains with localization files, choose one for this translation.")
                .setItemChoosenCallback {
                    if (swingList.selectedValue != null) {
                        val validPattern = Regex("^.*?/assets/${Regex.escape(swingList.selectedValue!!)}/lang.*?\$")
                        write(files.filter { validPattern.matches(it.path) })
                    }
                }
                .createPopup()
                .showInBestPositionFor(FileEditorManager.getInstance(module.project).selectedTextEditor!!)
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
