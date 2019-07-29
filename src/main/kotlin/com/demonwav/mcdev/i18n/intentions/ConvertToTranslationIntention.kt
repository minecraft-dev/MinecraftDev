/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.util.IncorrectOperationException

class ConvertToTranslationIntention : PsiElementBaseIntentionAction() {
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (element.parent is PsiLiteral) {
            val value = (element.parent as PsiLiteral).value as String?
            val result = Messages.showInputDialogWithCheckBox("Enter translation key:",
                "Convert String Literal to Translation",
                "Replace literal with call to I18n (only works on clients!)",
                true,
                true,
                Messages.getQuestionIcon(),
                null,
                object : InputValidatorEx {
                    override fun getErrorText(inputString: String): String? {
                        if (inputString.isEmpty())
                            return "Key must not be empty"
                        if (inputString.contains('='))
                            return "Key must not contain separator character ('=')"
                        return null
                    }

                    override fun checkInput(inputString: String): Boolean {
                        return !inputString.isEmpty() && !inputString.contains('=')
                    }

                    override fun canClose(inputString: String): Boolean {
                        return !inputString.isEmpty() && !inputString.contains('=')
                    }
                })
            val key = result.first
            val replaceLiteral = result.second
            if (key != null) {
                val editorFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return
                val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(editorFile)
                I18nElementFactory.addTranslation(
                    module,
                    result.getFirst(),
                    value
                )
                if (replaceLiteral) {
                    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
                    psi.runWriteAction {
                        val expression = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                            "net.minecraft.client.resources.I18n.format(\"$key\")",
                            element.context
                        )
                        if (psi.language === JavaLanguage.INSTANCE) {
                            JavaCodeStyleManager.getInstance(project)
                                .shortenClassReferences(element.parent.replace(expression))
                        } else {
                            element.parent.replace(expression)
                        }
                    }
                }
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement) =
        (element.parent as? PsiLiteral)?.value is String

    override fun getFamilyName() = "Convert string literal to translation"

    override fun getText() = "Convert string literal to translation"

    override fun startInWriteAction() = false
}
