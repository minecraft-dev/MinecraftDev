/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.demonwav.mcdev.i18n.reference.I18nGotoModel
import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls

class MissingFormatInspection : TranslationInspection() {
    @Nls
    override fun getDisplayName() = "Detect missing format arguments for translations"

    override fun checkElement(element: PsiElement?, holder: ProblemsHolder) {
        if (element is PsiLiteralExpression) {
            val result = LiteralTranslationIdentifier().identify(element)
            if (result != null && result.formattingError) {
                holder.registerProblem(element, "There are missing formatting arguments to satisfy '${result.text}'", ProblemHighlightType.GENERIC_ERROR, ChangeTranslationQuickFix)
            }
        }
    }

    companion object {
        private object ChangeTranslationQuickFix : LocalQuickFix {
            override fun getName() = "Use a different translation"

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                try {
                    val literal = descriptor.psiElement as PsiLiteralExpression
                    val translation = LiteralTranslationIdentifier().identify(literal)
                    val popup = ChooseByNamePopup.createPopup(project, I18nGotoModel(project, translation?.regexPattern), null)
                    popup.invoke(object : ChooseByNamePopupComponent.Callback() {
                        override fun elementChosen(element: Any) {
                            val selectedProperty = element as I18nProperty
                            object : WriteCommandAction.Simple<Unit>(project, literal.containingFile) {
                                @Throws(Throwable::class)
                                override fun run() {
                                    val match = translation?.regexPattern?.matchEntire(selectedProperty.key)
                                    val insertion = if (match == null || match.groups.size <= 1) selectedProperty.key else match.groupValues[1]
                                    literal.replace(
                                        JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("\"$insertion\"", literal.context)
                                    )
                                }
                            }.execute()
                        }
                    }, ModalityState.current(), false)
                } catch (ignored: IncorrectOperationException) {
                }
            }

            override fun startInWriteAction() = false

            override fun getFamilyName(): String {
                return name
            }
        }
    }
}
