/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.reference.I18nGotoModel
import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.IncorrectOperationException

class ChangeTranslationQuickFix(private val name: String) : LocalQuickFix {
    override fun getName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        try {
            val literal = descriptor.psiElement as PsiLiteralExpression
            val translation = LiteralTranslationIdentifier().identify(literal)
            val popup = ChooseByNamePopup.createPopup(project, I18nGotoModel(project, translation?.regexPattern), null)
            popup.invoke(object : ChooseByNamePopupComponent.Callback() {
                override fun elementChosen(element: Any) {
                    val selectedEntry = element as I18nEntry
                    literal.containingFile.runWriteAction {
                        val match = translation?.regexPattern?.matchEntire(selectedEntry.key)
                        val insertion =
                            if (match == null || match.groups.size <= 1) selectedEntry.key else match.groupValues[1]
                        literal.replace(
                            JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                "\"$insertion\"",
                                literal.context
                            )
                        )
                    }
                }
            }, ModalityState.current(), false)
        } catch (ignored: IncorrectOperationException) {
        }
    }

    override fun startInWriteAction() = false

    override fun getFamilyName() = name
}
