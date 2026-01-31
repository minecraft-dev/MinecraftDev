/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.demonwav.mcdev.translations.DeprecatedTranslations
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.translations.identification.TranslationInstance.FormattingError
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.merge
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.referencedMethod
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInspection.dataFlow.CommonDataflow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import java.util.IllegalFormatException
import java.util.MissingFormatArgumentException
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.util.isNewArrayWithInitializer

object TranslationIdentifier {
    fun identify(
        element: UExpression
    ): TranslationInstance? {
        val call = element.uastParent as? UCallExpression ?: return null
        val index = call.valueArguments.indexOf(element)

        val method = call.referencedMethod ?: return null
        val parameter = method.uastParameters.getOrNull(index) ?: return null
        val translatableAnnotation = AnnotationUtil.findAnnotation(
            parameter.javaPsi as PsiParameter,
            TranslationConstants.TRANSLATABLE_ANNOTATION
        ) ?: return null

        val project = element.sourcePsi?.project ?: return null

        val prefix =
            translatableAnnotation.findAttributeValue(TranslationConstants.PREFIX)?.constantStringValue ?: ""
        val suffix =
            translatableAnnotation.findAttributeValue(TranslationConstants.SUFFIX)?.constantStringValue ?: ""
        val required =
            translatableAnnotation.findAttributeValue(TranslationConstants.REQUIRED)?.constantValue as? Boolean
                ?: true
        val isPreEscapeException =
            method.getContainingUClass()?.qualifiedName?.startsWith("net.minecraft.") == true &&
                isPreEscapeMcVersion(project, element.sourcePsi!!)
        val allowArbitraryArgs = isPreEscapeException || translatableAnnotation.findAttributeValue(
            TranslationConstants.ALLOW_ARBITRARY_ARGS
        )?.constantValue as? Boolean ?: false

        val translationKey = when (val javaPsi = element.javaPsi) {
            is PsiExpression -> {
                RecursionManager.doPreventingRecursion(javaPsi, false) {
                    CommonDataflow.computeValue(javaPsi) as? String
                }
            }
            else -> element.evaluateString()
        }?.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "") ?: return null

        val deprecations = DeprecatedTranslations.getInstance(project)
        val key = prefix + translationKey + suffix

        val shouldFold = element is ULiteralExpression && element.isString && key !in deprecations.removed && key !in deprecations.renamed

        val entries = TranslationIndex.getAllDefaultEntries(project).merge("")
        val translation = entries[deprecations.inverseRenamed[key] ?: key]?.text
            ?: return TranslationInstance( // translation doesn't exist
                null,
                index,
                element,
                TranslationInstance.Key(prefix, translationKey, suffix),
                null,
                required,
                allowArbitraryArgs,
                shouldFold = shouldFold,
            )

        val foldMethod =
            translatableAnnotation.findAttributeValue(TranslationConstants.FOLD_METHOD)?.constantValue as? Boolean
                ?: false

        val formatting =
            (method.uastParameters.last().type as? PsiEllipsisType)
                ?.componentType?.equalsToText(CommonClassNames.JAVA_LANG_OBJECT) == true

        val foldingElement = if (foldMethod) {
            // Make sure qualifiers, like I18n in 'I18n.translate()' is also folded
            call.uastParent as? UQualifiedReferenceExpression ?: call
        } else if (
            index == 0 &&
            call.valueArgumentCount > 1 &&
            method.uastParameters.size == 2 &&
            formatting
        ) {
            call
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
                element,
                TranslationInstance.Key(prefix, translationKey, suffix),
                formatted,
                required,
                allowArbitraryArgs,
                if (superfluousParams >= 0) FormattingError.SUPERFLUOUS else null,
                superfluousParams,
                shouldFold = shouldFold,
            )
        } catch (_: MissingFormatArgumentException) {
            return TranslationInstance(
                foldingElement,
                index,
                element,
                TranslationInstance.Key(prefix, translationKey, suffix),
                translation,
                required,
                allowArbitraryArgs,
                FormattingError.MISSING,
                shouldFold = shouldFold,
            )
        }
    }

    private fun format(method: UMethod, translation: String, call: UCallExpression): Pair<String, Int>? {
        val format = NUMBER_FORMATTING_PATTERN.replace(translation, "%$1s")
        val paramCount = STRING_FORMATTING_PATTERN.findAll(format).count()

        val parametersCount = method.uastParameters.size
        val varargs = call.extractVarArgs(parametersCount - 1)
            ?: return null
        val varargStart = if (varargs.size > paramCount) {
            parametersCount - 1 + paramCount
        } else {
            -1
        }
        return try {
            String.format(format, *varargs) to varargStart
        } catch (e: MissingFormatArgumentException) {
            // rethrow this specific exception to be handled by the caller
            throw e
        } catch (_: IllegalFormatException) {
            null
        }
    }

    fun UCallExpression.extractVarArgs(index: Int): Array<String>? {
        val method = this.referencedMethod
        val args = this.valueArguments
        if (method == null || args.size < (index + 1)) {
            return emptyArray()
        }

        val psiParam = method.uastParameters[index].javaPsi as? PsiParameter
            ?: return null
        if (!psiParam.isVarArgs) {
            return arrayOf(args[index].paramDisplayString())
        }

        val elements = args.drop(index)
        return extractVarArgs(elements)?.mapToArray { it.paramDisplayString() }
    }

    private fun extractVarArgs(elements: List<UExpression>): List<UExpression>? {
        val arg = elements.singleOrNull() ?: return elements
        val arrayType = arg.getExpressionType() as? PsiArrayType ?: return elements
        if (arrayType.componentType is PsiPrimitiveType) return elements
        return if (arg is UCallExpression && arg.isNewArrayWithInitializer()) {
            // We're dealing with an array initializer, let's use its elements!
            arg.valueArguments
        } else {
            // We're dealing with a more complex expression that results in an array, give up
            null
        }
    }

    fun UExpression.paramDisplayString(): String {
        val visited = mutableSetOf<UExpression?>()

        fun eval(expr: UExpression, defaultValue: String = "\${${expr.asSourceString()}}"): String {
            if (!visited.add(expr)) {
                return defaultValue
            }

            when (expr) {
                is UQualifiedReferenceExpression -> {
                    val selector = expr.selector
                    if (selector is UCallExpression) {
                        return eval(selector, "\${${expr.asSourceString()}}")
                    }
                }

                is UCallExpression -> for (argument in expr.valueArguments) {
                    val translation = identify(argument) ?: continue
                    if (translation.formattingError == null) {
                        translation.text?.let { return it }
                    }
                }

                else -> expr.evaluateString()?.let { return it }
            }

            return defaultValue
        }

        return eval(this)
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
