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
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class SortTranslationsIntention(private val ordering: SortTranslationsIntention.Ordering, private val keepComments: Int) : BaseIntentionAction() {
    enum class Ordering {
        ASCENDING, DESCENDING, LIKE_DEFAULT
    }

    override fun getText() = "Sort translations"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val sorted = psiFile.children.mapNotNull { it as? I18nEntry }.let {
            when (ordering) {
                Ordering.ASCENDING -> it.sortedBy { it.key }
                Ordering.DESCENDING -> it.sortedByDescending { it.key }
                else -> {
                    val defaults = project.findDefaultLangEntries(
                        scope = Scope.PROJECT,
                        domain = I18nElementFactory.getResourceDomain(psiFile.virtualFile)
                    )
                    val indices = defaults.mapIndexed { i, prop -> prop.key to i }.toMap()
                    it.sortedBy { indices[it.key] ?: Int.MAX_VALUE }
                }
            }
        }

        tailrec fun gatherComments(element: PsiElement, acc: List<String> = emptyList(), depth: Int = 0): List<String> {
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
            return gatherComments(prevLine, listOf(prevLine.text.substring(1).trim()) + acc, depth + 1)
        }

        psiFile.runWriteAction {
            val withComments = sorted.associate { it to gatherComments(it) }
            for (elem in psiFile.children) {
                elem.delete()
            }
            for ((property, comments) in withComments) {
                for (comment in comments) {
                    psiFile.add(I18nElementFactory.createComment(project, comment))
                    psiFile.add(I18nElementFactory.createLineEnding(project))
                }
                psiFile.add(I18nElementFactory.createProperty(project, property.key, property.value))
                psiFile.add(I18nElementFactory.createLineEnding(project))
            }
        }
    }
}
