/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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
import com.demonwav.mcdev.util.computeReadAction
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.lexicographical
import com.demonwav.mcdev.util.mcDomain
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
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
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Sorting Translation File", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                sort(project, file, order, comments, indicator)
            }
        })
    }

    private inline fun <T> time(name: String, func: () -> T): T {
        val startTime = System.nanoTime()
        val result = func()
        val endTime = System.nanoTime()
        println("$name TIME: ${endTime - startTime}ns")
        return result
    }

    private fun sort(project: Project, file: PsiFile, ordering: Ordering, keepComments: Int, indicator: ProgressIndicator) {
        indicator.text2 = "Sorting keys..."
        val children = computeReadAction { file.children }
        val entries = children.mapNotNull { it as? I18nEntry }

        val sorted = time("READ") {
            computeReadAction {
                I18nElementFactory.createFile(
                    project,
                    entries.let {
                        when (ordering) {
                            Ordering.ASCENDING -> I18nElementFactory.assembleRawElements(it.sortedWith(ascendingComparator), keepComments)
                            Ordering.DESCENDING -> I18nElementFactory.assembleRawElements(it.sortedWith(descendingComparator), keepComments)
                            Ordering.TEMPLATE -> sortByTemplate(TemplateManager.getProjectTemplate(project), it, keepComments)
                            else -> sortByTemplate(buildDefaultTemplate(project, file.virtualFile.mcDomain)
                                ?: return@computeReadAction null, it, keepComments)
                        }
                    }).children
            }
        } ?: return

        invokeLater {
            file.runWriteAction {
                time("DELETE") {
                    file.deleteChildRange(children.first(), children.last())
                }
                time("WRITE") {
                    file.addRange(sorted.first(), sorted.last())
                }
            }
        }
    }

    private fun buildDefaultTemplate(project: Project, domain: String?): Template? {
        val referenceFile = project.findDefaultLangFile(domain) ?: return null
        val psi = PsiManager.getInstance(project).findFile(referenceFile) ?: return null
        val elements = mutableListOf<TemplateElement>()
        for (child in psi.children) {
            when {
                child is I18nEntry ->
                    elements.add(Key(Regex.escape(child.key).toRegex()))
                child.node.elementType == I18nTypes.LINE_ENDING && child.prevSibling.node.elementType == I18nTypes.LINE_ENDING ->
                    elements.add(EmptyLine)
            }
        }
        return Template(elements)
    }

    private fun sortByTemplate(template: Template, entries: List<I18nEntry>, keepComments: Int): String {
        val result = StringBuilder()
        val tmp = entries.toMutableList()
        for (elem in template.elements) {
            when (elem) {
                is Comment -> result.append("# ${elem.text}\n")
                EmptyLine -> result.append('\n')
                is Key -> {
                    val toWrite = tmp.filter { elem.matcher.matches(it.key) }
                    result.append(I18nElementFactory.assembleRawElements(toWrite.sortedWith(ascendingComparator), keepComments))
                    tmp.removeAll(toWrite)
                }
            }
        }
        if (tmp.isNotEmpty()) {
            result.append(I18nElementFactory.assembleRawElements(tmp.sortedWith(ascendingComparator), keepComments))
        }
        return result.toString()
    }
}
