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
import com.demonwav.mcdev.i18n.findDefaultProperties
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class SortTranslationsIntention(private val ordering: SortTranslationsIntention.Ordering) : BaseIntentionAction() {
    enum class Ordering {
        ASCENDING, DESCENDING, LIKE_DEFAULT
    }

    override fun getText() = "Sort translations"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        val sorted = psiFile.children.mapNotNull { it as? I18nProperty }.let {
            when (ordering) {
                Ordering.ASCENDING -> it.sortedBy { it.key }
                Ordering.DESCENDING -> it.sortedByDescending { it.key }
                else -> {
                    val defaults = project.findDefaultProperties(
                        scope = Scope.PROJECT,
                        domain = I18nElementFactory.getResourceDomain(psiFile.virtualFile)
                    )
                    val indices = defaults.mapIndexed { i, prop -> prop.key to i }.toMap()
                    it.sortedBy { indices[it.key] ?: Int.MAX_VALUE }
                }
            }
        }
        object : WriteCommandAction.Simple<Unit>(project, psiFile) {
            @Throws(Throwable::class)
            override fun run() {
                for (elem in psiFile.children) {
                    elem.delete()
                }
                var lastStart: Char = 0.toChar()
                for (property in sorted) {
                    if (lastStart.toInt() != 0 && property.key[0] != lastStart) {
                        psiFile.add(I18nElementFactory.createLineEnding(project))
                    }
                    psiFile.add(I18nElementFactory.createProperty(project, property.key, property.value))
                    psiFile.add(I18nElementFactory.createLineEnding(project))
                    lastStart = property.key[0]
                }
            }
        }.execute()
    }
}
