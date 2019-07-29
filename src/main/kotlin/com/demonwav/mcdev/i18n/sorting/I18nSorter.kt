/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.sorting

import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.actions.TranslationSortOrderDialog
import com.demonwav.mcdev.i18n.findDefaultLangFile
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.lexicographical
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

object I18nSorter {
    private val ascendingComparator = compareBy<I18nEntry, Iterable<String>>(
        naturalOrder<String>().lexicographical(),
        { it.key.split('.') }
    )
    private val descendingComparator = ascendingComparator.reversed()

    fun query(project: Project, file: PsiFile, defaultSelection: Ordering = Ordering.ASCENDING) {
        val defaultFileMissing = project.findDefaultLangFile(file.virtualFile.mcDomain ?: return) == null
        val isDefaultFile = file.name == I18nConstants.DEFAULT_LOCALE_FILE
        val (order, comments) = TranslationSortOrderDialog.show(defaultFileMissing || isDefaultFile, defaultSelection)
        if (order == null) {
            return
        }
        sort(project, file, order, comments)
    }

    private fun sort(project: Project, file: PsiFile, ordering: Ordering, keepComments: Int) {
        val sorted = file.children.mapNotNull { it as? I18nEntry }.let {
            when (ordering) {
                Ordering.ASCENDING -> I18nElementFactory.assembleElements(
                    project,
                    it.sortedWith(ascendingComparator),
                    keepComments
                )
                Ordering.DESCENDING -> I18nElementFactory.assembleElements(
                    project,
                    it.sortedWith(descendingComparator),
                    keepComments
                )
                Ordering.TEMPLATE -> sortByTemplate(
                    project,
                    TemplateManager.getProjectTemplate(project),
                    it,
                    keepComments
                )
                else -> sortByTemplate(
                    project,
                    buildDefaultTemplate(project, file.virtualFile.mcDomain) ?: return,
                    it,
                    keepComments
                )
            }
        }

        file.runWriteAction {
            for (elem in file.children) {
                elem.delete()
            }
            for (elem in sorted) {
                file.add(elem)
            }
        }
    }

    private fun buildDefaultTemplate(project: com.intellij.openapi.project.Project, domain: String?): Template? {
        val referenceFile = project.findDefaultLangFile(domain) ?: return null
        val psi = PsiManager.getInstance(project).findFile(referenceFile) ?: return null
        val elements = mutableListOf<TemplateElement>()
        for (child in psi.children) {
            when {
                child is I18nEntry ->
                    elements.add(Key(Regex.escape(child.key).toRegex()))
                child.node.elementType == I18nTypes.LINE_ENDING &&
                    child.prevSibling.node.elementType == I18nTypes.LINE_ENDING ->
                    elements.add(EmptyLine)
            }
        }
        return Template(elements)
    }

    private fun sortByTemplate(
        project: com.intellij.openapi.project.Project,
        template: Template,
        entries: List<I18nEntry>,
        keepComments: Int
    ): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val tmp = entries.toMutableList()
        for (elem in template.elements) {
            when (elem) {
                is Comment -> {
                    result.add(I18nElementFactory.createComment(project, elem.text))
                    result.add(I18nElementFactory.createLineEnding(project))
                }
                EmptyLine -> result.add(I18nElementFactory.createLineEnding(project))
                is Key -> {
                    val toWrite = tmp.filter { elem.matcher.matches(it.key) }
                    result.addAll(
                        I18nElementFactory.assembleElements(
                            project,
                            toWrite.sortedWith(ascendingComparator),
                            keepComments
                        )
                    )
                    tmp.removeAll(toWrite)
                }
            }
        }
        if (tmp.isNotEmpty()) {
            result.addAll(
                I18nElementFactory.assembleElements(
                    project,
                    tmp.sortedWith(ascendingComparator),
                    keepComments
                )
            )
        }
        return result
    }
}
