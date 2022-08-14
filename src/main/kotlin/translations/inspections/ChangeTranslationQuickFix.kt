/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.inspections

import com.demonwav.mcdev.translations.identification.LiteralTranslationIdentifier
import com.demonwav.mcdev.translations.reference.TranslationGotoModel
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNamedElement
import com.intellij.util.IncorrectOperationException

class ChangeTranslationQuickFix(private val name: String) : LocalQuickFix {
    override fun getName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        try {
            val literal = descriptor.psiElement as PsiLiteralExpression
            val key = LiteralTranslationIdentifier().identify(literal)?.key ?: return
            val popup = ChooseByNamePopup.createPopup(
                project,
                TranslationGotoModel(project, key.prefix, key.suffix),
                null
            )
            popup.invoke(
                object : ChooseByNamePopupComponent.Callback() {
                    override fun elementChosen(element: Any) {
                        val selectedKey = (element as PsiNamedElement).name ?: return
                        literal.containingFile.runWriteAction {
                            val insertion = selectedKey.substring(
                                key.prefix.length,
                                selectedKey.length - key.suffix.length
                            )
                            literal.replace(
                                JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText(
                                    "\"$insertion\"",
                                    literal.context
                                )
                            )
                        }
                    }
                },
                ModalityState.current(),
                false
            )
        } catch (ignored: IncorrectOperationException) {
        }
    }

    override fun startInWriteAction() = false

    override fun getFamilyName() = name
}
