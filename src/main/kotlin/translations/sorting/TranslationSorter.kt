/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.sorting

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.actions.TranslationSortOrderDialog
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.util.lexicographical
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

object TranslationSorter {
    private val ascendingComparator = compareBy<Translation, Iterable<String>>(
        naturalOrder<String>().lexicographical()
    ) { it.key.split('.') }

    private val descendingComparator = ascendingComparator.reversed()

    fun query(project: Project, file: PsiFile, defaultSelection: Ordering = Ordering.ASCENDING) {
        val domain = file.virtualFile.mcDomain
        val defaultEntries = TranslationIndex.getProjectDefaultTranslations(project, domain)
        val noDefaults = defaultEntries.none()
        val isDefaultFile = TranslationFiles.getLocale(file.virtualFile) == TranslationConstants.DEFAULT_LOCALE
        val (order, comments) = TranslationSortOrderDialog.show(noDefaults || isDefaultFile, defaultSelection)

        if (order == null) {
            return
        }

        sort(project, file, order, comments)
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
                    it.sortedWith(ascendingComparator),
                    keepComments
                )
                Ordering.DESCENDING -> TranslationFiles.buildFileEntries(
                    project,
                    locale,
                    it.sortedWith(descendingComparator),
                    keepComments
                )
                Ordering.TEMPLATE -> sortByTemplate(
                    project,
                    locale,
                    TemplateManager.getProjectTemplate(project),
                    it,
                    keepComments
                )
                else -> sortByTemplate(
                    project,
                    locale,
                    TranslationFiles.buildSortingTemplateFromDefault(file, domain)
                        ?: throw IllegalStateException("Could not generate template from default translation file"),
                    it,
                    keepComments
                )
            }
        }

        file.runWriteAction {
            TranslationFiles.replaceAll(file, sorted.asIterable())
        }
    }

    private fun sortByTemplate(
        project: Project,
        locale: String,
        template: Template,
        entries: Sequence<Translation>,
        keepComments: Int
    ) = sequence {
        val tmp = entries.toMutableList()

        for (elem in template.elements) {
            when (elem) {
                is Comment -> yield(TranslationFiles.FileEntry.Comment(elem.text))
                EmptyLine -> yield(TranslationFiles.FileEntry.EmptyLine)
                is Key -> {
                    val toWrite = tmp.asSequence().filter { elem.matcher.matches(it.key) }
                    yieldAll(
                        TranslationFiles.buildFileEntries(
                            project,
                            locale,
                            toWrite.sortedWith(ascendingComparator),
                            keepComments
                        )
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
                    tmp.sortedWith(ascendingComparator).asSequence(),
                    keepComments
                )
            )
        }
    }
}
