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

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.platform.mcp.mappings.getMappedClass
import com.demonwav.mcdev.platform.mcp.mappings.getMappedMethod
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.merge
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.extractVarArgs
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.referencedMethod
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiMethod
import java.util.IllegalFormatException
import java.util.MissingFormatArgumentException

abstract class TranslationIdentifier<T : PsiElement> {
    @Suppress("UNCHECKED_CAST")
    fun identifyUnsafe(element: PsiElement): TranslationInstance? {
        return identify(element as T)
    }

    abstract fun identify(element: T): TranslationInstance?

    abstract fun elementClass(): Class<T>

    companion object {
        val INSTANCES = listOf(LiteralTranslationIdentifier(), ReferenceTranslationIdentifier())

        fun identify(
            project: Project,
            element: PsiExpression,
            container: PsiElement,
            referenceElement: PsiElement,
        ): TranslationInstance? {
            if (container !is PsiExpressionList) {
                return null
            }
            val call = container.parent as? PsiCall ?: return null
            val index = container.expressions.indexOf(element)

            val method = call.referencedMethod ?: return null
            val parameter = method.parameterList.getParameter(index) ?: return null
            val translatableAnnotation =
                AnnotationUtil.findAnnotation(parameter, TranslationConstants.TRANSLATABLE_ANNOTATION) ?: return null

            val prefix =
                translatableAnnotation.findAttributeValue(TranslationConstants.PREFIX)?.constantStringValue ?: ""
            val suffix =
                translatableAnnotation.findAttributeValue(TranslationConstants.SUFFIX)?.constantStringValue ?: ""
            val required =
                translatableAnnotation.findAttributeValue(TranslationConstants.REQUIRED)?.constantValue as? Boolean
                    ?: true
            val isPreEscapeException =
                method.containingClass?.qualifiedName?.startsWith("net.minecraft.") == true &&
                    isPreEscapeMcVersion(project, element)
            val allowArbitraryArgs = isPreEscapeException || translatableAnnotation.findAttributeValue(
                TranslationConstants.ALLOW_ARBITRARY_ARGS
            )?.constantValue as? Boolean ?: false

            val translationKey = CommonDataflow.computeValue(element) as? String ?: return null

            val entries = TranslationIndex.getAllDefaultEntries(project).merge("")
            val translation = entries[prefix + translationKey + suffix]?.text
                ?: return TranslationInstance( // translation doesn't exist
                    null,
                    index,
                    referenceElement,
                    TranslationInstance.Key(prefix, translationKey, suffix),
                    null,
                    required,
                    allowArbitraryArgs,
                )

            val foldMethod =
                translatableAnnotation.findAttributeValue(TranslationConstants.FOLD_METHOD)?.constantValue as? Boolean
                    ?: false

            val formatting =
                (method.parameterList.parameters.last().type as? PsiEllipsisType)
                    ?.componentType?.equalsToText(CommonClassNames.JAVA_LANG_OBJECT) == true

            val foldingElement = if (foldMethod) {
                call
            } else if (
                index == 0 &&
                container.expressionCount > 1 &&
                method.parameterList.parametersCount == 2 &&
                formatting
            ) {
                container
            } else {
                element
            }
            try {
                val (formatted, superfluousParams) = if (formatting) {
                    format(method, translation, call) ?: (translation to -1)
                } else {
                    (translation to -1)
                }
                return TranslationInstance(
                    foldingElement,
                    index,
                    referenceElement,
                    TranslationInstance.Key(prefix, translationKey, suffix),
                    formatted,
                    required,
                    allowArbitraryArgs,
                    if (superfluousParams >= 0) FormattingError.SUPERFLUOUS else null,
                    superfluousParams,
                )
            } catch (ignored: MissingFormatArgumentException) {
                return TranslationInstance(
                    foldingElement,
                    index,
                    referenceElement,
                    TranslationInstance.Key(prefix, translationKey, suffix),
                    translation,
                    required,
                    allowArbitraryArgs,
                    FormattingError.MISSING,
                )
            }
        }

        private fun format(method: PsiMethod, translation: String, call: PsiCall): Pair<String, Int>? {
            val format = NUMBER_FORMATTING_PATTERN.replace(translation, "%$1s")
            val paramCount = STRING_FORMATTING_PATTERN.findAll(format).count()

            val varargs = call.extractVarArgs(method.parameterList.parametersCount - 1, true, true)
                ?: return null
            val varargStart = if (varargs.size > paramCount) {
                method.parameterList.parametersCount - 1 + paramCount
            } else {
                -1
            }
            return try {
                String.format(format, *varargs) to varargStart
            } catch (e: MissingFormatArgumentException) {
                // rethrow this specific exception to be handled by the caller
                throw e
            } catch (e: IllegalFormatException) {
                null
            }
        }

        private fun isPreEscapeMcVersion(project: Project, contextElement: PsiElement): Boolean {
            val module = contextElement.findModule() ?: return false
            val componentClassName = module.getMappedClass("net.minecraft.network.chat.Component")
            val componentClass = JavaPsiFacade.getInstance(project)
                .findClass(componentClassName, contextElement.resolveScope) ?: return false
            val translatableEscapeName = module.getMappedMethod(
                "net.minecraft.network.chat.Component",
                "translatableEscape",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/Component;"
            )
            return componentClass.findMethodsByName(translatableEscapeName, false).any { method ->
                method.descriptor?.startsWith("(Ljava/lang/String;[Ljava/lang/Object;)") == true
            }
        }

        private val NUMBER_FORMATTING_PATTERN = Regex("%(\\d+\\$)?[\\d.]*[df]")
        private val STRING_FORMATTING_PATTERN = Regex("[^%]?%(?:\\d+\\$)?s")
    }
}
