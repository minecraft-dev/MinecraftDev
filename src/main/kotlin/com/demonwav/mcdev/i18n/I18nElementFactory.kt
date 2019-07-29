/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.mcDomain
import com.intellij.ide.DataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.Locale

object I18nElementFactory {
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
                        add(createEntry(project, name, value))
                    }
                }
            }
        }

        val files = FileTypeIndex.getFiles(I18nFileType, GlobalSearchScope.moduleScope(module))
        if (files.count { it.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE } > 1) {
            val choices = files.mapNotNull { it.mcDomain }.distinct().sorted()
            DataManager.getInstance().dataContextFromFocusAsync.onSuccess {
                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(choices)
                    .setTitle("Choose resource domain")
                    .setAdText(
                        "There are multiple resource domains with localization files, choose one for this translation."
                    )
                    .setItemChosenCallback {
                        val validPattern = Regex("^.*?/assets/${Regex.escape(it)}/lang.*?\$")
                        write(files.filter { validPattern.matches(it.path) })
                    }
                    .createPopup()
                    .showInBestPositionFor(it)
            }
        } else {
            write(files)
        }
    }

    fun assembleElements(project: Project, elements: Collection<I18nEntry>, keepComments: Int): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val withComments = elements.associate { it to gatherComments(it, keepComments) }
        for ((entry, comments) in withComments) {
            for (comment in comments.asReversed()) {
                result.add(createComment(project, comment))
                result.add(createLineEnding(project))
            }
            result.add(createEntry(project, entry.key, entry.value))
            result.add(createLineEnding(project))
        }
        return result
    }

    private tailrec fun gatherComments(
        element: PsiElement,
        maxDepth: Int,
        acc: MutableList<String> = mutableListOf(),
        depth: Int = 0
    ): List<String> {
        if (maxDepth != 0 && depth >= maxDepth) {
            return acc
        }
        val prev = element.prevSibling ?: return acc
        if (prev.node.elementType != I18nTypes.LINE_ENDING) {
            return acc
        }
        val prevLine = prev.prevSibling ?: return acc
        if (prevLine.node.elementType != I18nTypes.COMMENT) {
            return acc
        }
        acc.add(prevLine.text.substring(1).trim())
        return gatherComments(prevLine, maxDepth, acc, depth + 1)
    }

    fun createFile(project: Project, text: String): I18nFile {
        return PsiFileFactory.getInstance(project).createFileFromText("name", I18nFileType, text) as I18nFile
    }

    fun createComment(project: Project, text: String): PsiElement {
        val file = createFile(project, "# $text")
        return file.firstChild
    }

    fun createEntry(project: Project, key: String, value: String = ""): I18nEntry {
        val file = createFile(project, "$key=$value")
        return file.firstChild as I18nEntry
    }

    fun createLineEnding(project: Project): PsiElement {
        val file = createFile(project, "\n")
        return file.firstChild
    }
}
