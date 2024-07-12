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
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.findUElementAt
import org.jetbrains.uast.generate.generationPlugin
import org.jetbrains.uast.textRange
import org.jetbrains.uast.toUElementOfType

class ConvertToTranslationIntention : PsiElementBaseIntentionAction() {
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val literal = element.parent.toUElementOfType<ULiteralExpression>() ?: return
        val value = literal.evaluateString() ?: return

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
                val documentManager = PsiDocumentManager.getInstance(project)
                val psi = documentManager.getPsiFile(editor.document) ?: return
                val callCode = if (translationSettings.isUseCustomConvertToTranslationTemplate) {
                    translationSettings.convertToTranslationTemplate.replace("\$key", key)
                } else {
                    element.findModule()?.getMappedMethodCall(
                        "net.minecraft.client.resource.language.I18n",
                        "translate",
                        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                        "\"$key\""
                    ) ?: "net.minecraft.client.resource.I18n.get(\"$key\")"
                }

                val replaceRange = when (literal.lang.id) {
                    // Special case because in Kotlin, the sourcePsi is a template entry, not the literal itself
                    "kotlin" -> literal.sourcePsi?.parent?.textRange
                    else -> literal.textRange
                } ?: return

                psi.runWriteAction {
                    // There is no convenient way to generate a qualified call expression with the UAST factory
                    // so we simply put the raw code there and assume it's correct
                    editor.document.replaceString(replaceRange.startOffset, replaceRange.endOffset, callCode)
                    documentManager.commitDocument(editor.document)

                    val callOffset = replaceRange.startOffset + callCode.indexOf('(')
                    val newExpr = psi.findUElementAt(callOffset - 1, UReferenceExpression::class.java)
                    if (newExpr != null) {
                        literal.generationPlugin?.shortenReference(newExpr)
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

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val literal = element.parent.toUElementOfType<ULiteralExpression>()
        return literal?.evaluateString() is String
    }

    override fun getFamilyName() = "Convert string literal to translation"

    override fun getText() = "Convert string literal to translation"

    override fun startInWriteAction() = false
}
