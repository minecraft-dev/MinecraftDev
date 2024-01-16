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

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.actions.TranslationSortOrderDialog
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.util.applyWriteAction
import com.demonwav.mcdev.util.lexicographical
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import java.util.TreeSet

object TranslationSorter {
    private val ascendingComparator = compareBy<Translation, Iterable<String>>(
        naturalOrder<String>().lexicographical(),
    ) { it.key.split('.') }

    private val descendingComparator = ascendingComparator.reversed()

    fun query(
        project: Project,
        file: PsiFile,
        hasDefaultTranslations: Boolean,
        defaultSelection: Ordering = Ordering.ASCENDING
    ) {
        val (order, comments) = TranslationSortOrderDialog.show(
            hasDefaultTranslations || TranslationFiles.isDefaultLocale(file.virtualFile),
            defaultSelection
        )

        if (order == null) {
            return
        }

        file.applyWriteAction { sort(project, file, order, comments) }
    }

    private fun sort(project: Project, file: PsiFile, ordering: Ordering, keepComments: Int) {
        val domain = file.virtualFile.mcDomain
        val locale = TranslationFiles.getLocale(file.virtualFile) ?: return
        val translations = TranslationIndex.getTranslations(file)
        val sorted = translations.let {
            when (ordering) {
                Ordering.ASCENDING -> TranslationFiles.buildFileEntries(
                    project,
                    locale,
                    it.sortedWith(ascendingComparator).asIterable(),
                    keepComments,
                )
                Ordering.DESCENDING -> TranslationFiles.buildFileEntries(
                    project,
                    locale,
                    it.sortedWith(descendingComparator).asIterable(),
                    keepComments,
                )
                Ordering.TEMPLATE -> sortByTemplate(
                    project,
                    locale,
                    TemplateManager.getProjectTemplate(project),
                    it,
                    keepComments,
                )
                else -> sortByTemplate(
                    project,
                    locale,
                    TranslationFiles.buildSortingTemplateFromDefault(file, domain)
                        ?: throw IllegalStateException("Could not generate template from default translation file"),
                    it,
                    keepComments,
                )
            }
        }

        file.runWriteAction {
            TranslationFiles.replaceAll(file, sorted.asIterable())
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file)
            if (document != null) {
                documentManager.commitDocument(document)
                CodeStyleManager.getInstance(project).reformat(file, true)
            }
        }
    }

    private fun sortByTemplate(
        project: Project,
        locale: String,
        template: Template,
        entries: Sequence<Translation>,
        keepComments: Int,
    ) = sequence {
        val tmp = entries.toMutableList()

        for (elem in template.elements) {
            when (elem) {
                is Comment -> yield(TranslationFiles.FileEntry.Comment(elem.text))
                EmptyLine -> yield(TranslationFiles.FileEntry.EmptyLine)
                is Key -> {
                    val toWrite = tmp.filterTo(TreeSet(ascendingComparator)) { elem.matcher.matches(it.key) }
                    yieldAll(
                        TranslationFiles.buildFileEntries(
                            project,
                            locale,
                            toWrite,
                            keepComments,
                        ),
                    )
                    tmp.removeAll(toWrite)
                }
            }
        }

        if (tmp.isNotEmpty()) {
            yieldAll(
                TranslationFiles.buildFileEntries(
                    project,
                    locale,
                    tmp.sortedWith(ascendingComparator),
                    keepComments,
                ),
            )
        }
    }
}
