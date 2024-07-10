/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.TranslationSettings
import com.demonwav.mcdev.platform.mcp.mappings.getMappedMethodCall
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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
            val value = (element.parent as PsiLiteral).value as? String ?: return

            val existingKey = TranslationFiles.findTranslationKeyForText(element, value)

            val result = Messages.showInputDialogWithCheckBox(
                "Enter translation key:",
                "Convert String Literal to Translation",
                "Replace literal with call to I18n (only works on clients!)",
                true,
                true,
                Messages.getQuestionIcon(),
                existingKey,
                object : InputValidatorEx {
                    override fun getErrorText(inputString: String): String? {
                        if (inputString.isEmpty()) {
                            return "Key must not be empty"
                        }
                        if (inputString.contains('=')) {
                            return "Key must not contain separator character ('=')"
                        }
                        return null
                    }

                    override fun checkInput(inputString: String): Boolean {
                        return inputString.isNotEmpty() && !inputString.contains('=')
                    }

                    override fun canClose(inputString: String): Boolean {
                        return inputString.isNotEmpty() && !inputString.contains('=')
                    }
                },
            )
            val key = result.first ?: return
            val replaceLiteral = result.second
            try {
                if (existingKey != key) {
                    TranslationFiles.add(element, key, value)
                }
                if (replaceLiteral) {
                    val translationSettings = TranslationSettings.getInstance(project)
                    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
                    psi.runWriteAction {
                        val expression = JavaPsiFacade.getElementFactory(project).createExpressionFromText(
                            if (translationSettings.isUseCustomConvertToTranslationTemplate) {
                                translationSettings.convertToTranslationTemplate.replace("\$key", key)
                            } else {
                                element.findModule()?.getMappedMethodCall(
                                    "net.minecraft.client.resource.language.I18n",
                                    "translate",
                                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                                    "\"$key\""
                                ) ?: "net.minecraft.client.resource.I18n.get(\"$key\")"
                            },
                            element.context,
                        )
                        if (psi.language === JavaLanguage.INSTANCE) {
                            JavaCodeStyleManager.getInstance(project)
                                .shortenClassReferences(element.parent.replace(expression))
                        } else {
                            element.parent.replace(expression)
                        }
                    }
                }
            } catch (e: Exception) {
                Notification(
                    "Translation support error",
                    "Error while adding translation",
                    e.message ?: e.stackTraceToString(),
                    NotificationType.WARNING,
                ).notify(project)
            }
        }
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement) =
        (element.parent as? PsiLiteral)?.value is String

    override fun getFamilyName() = "Convert string literal to translation"

    override fun getText() = "Convert string literal to translation"

    override fun startInWriteAction() = false
}
