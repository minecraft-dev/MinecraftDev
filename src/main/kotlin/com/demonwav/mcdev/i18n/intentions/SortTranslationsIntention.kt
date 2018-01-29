/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.Scope
import com.demonwav.mcdev.i18n.findDefaultLangEntries
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.i18n.sorting.Comment
import com.demonwav.mcdev.i18n.sorting.EmptyLine
import com.demonwav.mcdev.i18n.sorting.Key
import com.demonwav.mcdev.i18n.sorting.Template
import com.demonwav.mcdev.i18n.sorting.TemplateManager
import com.demonwav.mcdev.util.lexicographical
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class SortTranslationsIntention(private val ordering: SortTranslationsIntention.Ordering, private val keepComments: Int) : BaseIntentionAction() {
    enum class Ordering {
        ASCENDING, DESCENDING, LIKE_DEFAULT, TEMPLATE
    }

    private val ascendingComparator = compareBy<I18nEntry, Iterable<String>>(
        naturalOrder<String>().lexicographical(),
        { it.key.split('.') }
    )
    private val descendingComparator = ascendingComparator.reversed()

    override fun getText() = "Sort translations"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val sorted = psiFile.children.mapNotNull { it as? I18nEntry }.let {
            when (ordering) {
                Ordering.ASCENDING -> assembleElements(project, it.sortedWith(ascendingComparator))
                Ordering.DESCENDING -> assembleElements(project, it.sortedWith(descendingComparator))
                Ordering.TEMPLATE -> sortByTemplate(project, TemplateManager.getProjectTemplate(project), it)
                else -> {
                    val defaults = project.findDefaultLangEntries(
                        scope = Scope.PROJECT,
                        domain = psiFile.virtualFile.mcDomain
                    )
                    val indices = defaults.mapIndexed { i, prop -> prop.key to i }.toMap()
                    assembleElements(project, it.sortedBy { indices[it.key] ?: Int.MAX_VALUE })
                }
            }
        }

        psiFile.runWriteAction {
            for (elem in psiFile.children) {
                elem.delete()
            }
            for (elem in sorted) {
                psiFile.add(elem)
            }
        }
    }

    private fun sortByTemplate(project: Project, template: Template, entries: List<I18nEntry>): List<PsiElement> {
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
                    result.addAll(assembleElements(project, toWrite.sortedWith(ascendingComparator)))
                    tmp.removeAll(toWrite)
                }
            }
        }
        if (tmp.isNotEmpty()) {
            result.addAll(assembleElements(project, tmp.sortedWith(ascendingComparator)))
        }
        return result
    }

    private fun assembleElements(project: Project, elements: List<I18nEntry>): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        val withComments = elements.associate { it to gatherComments(it) }
        for ((entry, comments) in withComments) {
            for (comment in comments.asReversed()) {
                result.add(I18nElementFactory.createComment(project, comment))
                result.add(I18nElementFactory.createLineEnding(project))
            }
            result.add(I18nElementFactory.createEntry(project, entry.key, entry.value))
            result.add(I18nElementFactory.createLineEnding(project))
        }
        return result
    }

    private tailrec fun gatherComments(element: PsiElement, acc: MutableList<String> = mutableListOf(), depth: Int = 0): List<String> {
        if (keepComments != 0 && depth >= keepComments) {
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
        return gatherComments(prevLine, acc, depth + 1)
    }
}
