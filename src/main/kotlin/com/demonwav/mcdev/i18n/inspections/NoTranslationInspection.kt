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

import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.demonwav.mcdev.i18n.reference.I18nGotoModel
import com.demonwav.mcdev.i18n.reference.I18nReference
import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls

class NoTranslationInspection : TranslationInspection() {
    private val createQuickFix = CreateTranslationQuickFix()
    private val changeQuickFix = ChangeTranslationQuickFix()

    @Nls
    override fun getDisplayName() = "Detect missing translation"

    override fun getStaticDescription() =
        "Checks whether a translation key used in calls to <code>StatCollector.translateToLocal()</code>, " +
            "<code>StatCollector.translateToLocalFormatted()</code> or <code>I18n.format()</code> exists."

    override fun checkElement(element: PsiElement?, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (element is PsiLiteralExpression) {
            val result = LiteralTranslationIdentifier().identify(element)
            if (result != null && !result.containsVariable && result.text == null) {
                return arrayOf(manager.createProblemDescriptor(element,
                    "The given translation key does not exist",
                    arrayOf(createQuickFix, changeQuickFix),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly,
                    false))
            }
        }
        return null
    }

    private class CreateTranslationQuickFix : LocalQuickFix {
        override fun getName() = "Create translation"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                val literal = descriptor.psiElement as PsiLiteralExpression
                val translation = LiteralTranslationIdentifier().identify(literal)
                val literalValue = literal.value as String
                val key = translation?.varKey?.replace(I18nReference.VARIABLE_MARKER, literalValue) ?: literalValue
                val result = Messages.showInputDialog("Enter default value for \"$key\":",
                    "Create Translation",
                    Messages.getQuestionIcon())
                if (result != null) {
                    I18nElementFactory.addTranslation(
                        ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(literal.containingFile.virtualFile),
                        key,
                        result
                    )
                }
            } catch (ignored: IncorrectOperationException) {
            }

        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }

    private class ChangeTranslationQuickFix : LocalQuickFix {
        override fun getName() = "Use existing translation"

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
                                literal.replace(JavaPsiFacade.getInstance(project).elementFactory.createExpressionFromText("\"$insertion\"", literal.context))
                            }
                        }.execute()
                    }
                }, ModalityState.current(), false)
            } catch (ignored: IncorrectOperationException) {
            }
        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }
}
