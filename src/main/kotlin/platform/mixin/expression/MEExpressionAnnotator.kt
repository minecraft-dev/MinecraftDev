/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEBinaryExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEBoundMethodReferenceExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEDeclaration
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEDeclarationItem
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEFreeMethodReferenceExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MELitExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMemberAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENewExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStaticMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MESuperCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.findMultiInjectionHost
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.util.parentOfType

class MEExpressionAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MEDeclaration -> {
                val parent = element.parent as? MEDeclarationItem ?: return
                if (parent.isType) {
                    highlightDeclaration(holder, element, MEExpressionSyntaxHighlighter.IDENTIFIER_TYPE_DECLARATION)
                } else {
                    highlightDeclaration(holder, element, MEExpressionSyntaxHighlighter.IDENTIFIER_DECLARATION)
                }
            }
            is MEName -> {
                if (!element.isWildcard) {
                    when (val parent = element.parent) {
                        is METype,
                        is MENewExpression -> highlightType(holder, element)
                        is MEMemberAccessExpression -> highlightVariable(
                            holder,
                            element,
                            MEExpressionSyntaxHighlighter.IDENTIFIER_MEMBER_NAME,
                            true,
                        )
                        is MESuperCallExpression,
                        is MEMethodCallExpression,
                        is MEStaticMethodCallExpression,
                        is MEBoundMethodReferenceExpression,
                        is MEFreeMethodReferenceExpression -> highlightVariable(
                            holder,
                            element,
                            MEExpressionSyntaxHighlighter.IDENTIFIER_CALL,
                            false,
                        )
                        is MENameExpression -> {
                            if (METypeUtil.isExpressionDirectlyInTypePosition(parent)) {
                                highlightType(holder, element)
                            } else {
                                highlightVariable(
                                    holder,
                                    element,
                                    MEExpressionSyntaxHighlighter.IDENTIFIER_VARIABLE,
                                    false,
                                )
                            }
                        }
                        else -> highlightType(holder, element)
                    }
                }
            }
            is MELitExpression -> {
                val minusToken = element.minusToken
                if (minusToken != null) {
                    holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                        .range(minusToken)
                        .textAttributes(MEExpressionSyntaxHighlighter.NUMBER)
                        .create()
                }

                if (!element.isNull && !element.isString && element.value == null) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        MCDevBundle("mixinextras.expression.lang.errors.invalid_number")
                    )
                        .range(element)
                        .create()
                }
            }
            is MEBinaryExpression -> {
                val rightExpr = element.rightExpr
                if (element.operator == MEExpressionTypes.TOKEN_INSTANCEOF &&
                    rightExpr !is MENameExpression &&
                    rightExpr !is MEArrayAccessExpression &&
                    rightExpr != null
                ) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        MCDevBundle("mixinextras.expression.lang.errors.instanceof_non_type")
                    )
                        .range(rightExpr)
                        .create()
                }
            }
            is MEArrayAccessExpression -> {
                if (METypeUtil.isExpressionDirectlyInTypePosition(element)) {
                    val indexExpr = element.indexExpr
                    if (indexExpr != null) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            MCDevBundle("mixinextras.expression.lang.errors.index_not_expected_in_type"),
                        )
                            .range(indexExpr)
                            .create()
                    }
                    val arrayExpr = element.arrayExpr
                    if (arrayExpr !is MEArrayAccessExpression && arrayExpr !is MENameExpression) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            MCDevBundle("mixinextras.expression.lang.errors.instanceof_non_type"),
                        )
                            .range(arrayExpr)
                            .create()
                    }
                } else if (element.indexExpr == null) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        MCDevBundle("mixinextras.expression.lang.errors.array_access_missing_index"),
                    )
                        .range(element.leftBracketToken)
                        .create()
                }
            }
            is MENewExpression -> {
                if (element.isArrayCreation) {
                    val initializer = element.arrayInitializer
                    if (initializer != null) {
                        if (element.dimExprs.isNotEmpty()) {
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                MCDevBundle("mixinextras.expression.lang.errors.new_array_dim_expr_with_initializer"),
                            )
                                .range(initializer)
                                .create()
                        } else if (initializer.expressionList.isEmpty()) {
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                MCDevBundle("mixinextras.expression.lang.errors.empty_array_initializer"),
                            )
                                .range(initializer)
                                .create()
                        }
                    } else {
                        if (element.dimExprs.isEmpty()) {
                            holder.newAnnotation(
                                HighlightSeverity.ERROR,
                                MCDevBundle("mixinextras.expression.lang.errors.missing_array_length")
                            )
                                .range(element.dimExprTokens[0].leftBracket)
                                .create()
                        } else {
                            element.dimExprTokens.asSequence().dropWhile { it.expr != null }.forEach {
                                if (it.expr != null) {
                                    holder.newAnnotation(
                                        HighlightSeverity.ERROR,
                                        MCDevBundle("mixinextras.expression.lang.errors.array_length_after_empty")
                                    )
                                        .range(it.expr)
                                        .create()
                                }
                            }
                        }
                    }
                } else if (!element.hasConstructorArguments) {
                    val type = element.type
                    if (type != null) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            MCDevBundle("mixinextras.expression.lang.errors.new_no_constructor_args_or_array"),
                        )
                            .range(type)
                            .create()
                    }
                }
            }
        }
    }

    private fun highlightDeclaration(
        holder: AnnotationHolder,
        declaration: MEDeclaration,
        defaultColor: TextAttributesKey,
    ) {
        val isUnused = ReferencesSearch.search(declaration).findFirst() == null

        if (isUnused) {
            val message = MCDevBundle("mixinextras.expression.lang.errors.unused_definition")
            val annotation = holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(declaration)
                .highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL)

            val containingAnnotation = declaration.findMultiInjectionHost()?.parentOfType<PsiAnnotation>()?.takeIf {
                it.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)
            }
            if (containingAnnotation != null) {
                val inspectionManager = InspectionManager.getInstance(containingAnnotation.project)
                @Suppress("StatefulEp") // IntelliJ is wrong here
                val fix = object : RemoveAnnotationQuickFix(
                    containingAnnotation,
                    containingAnnotation.parentOfType<PsiModifierListOwner>()
                ) {
                    override fun getFamilyName() = MCDevBundle("mixinextras.expression.lang.errors.unused_symbol.fix")
                }
                val problemDescriptor = inspectionManager.createProblemDescriptor(
                    declaration,
                    message,
                    fix,
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    true
                )
                annotation.newLocalQuickFix(fix, problemDescriptor).registerFix()
            }

            annotation.create()
        } else {
            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                .range(declaration)
                .textAttributes(defaultColor)
                .create()
        }
    }

    private fun highlightType(holder: AnnotationHolder, type: MEName) {
        val typeName = type.text
        val isPrimitive = typeName != "void" && TypeConversionUtil.isPrimitive(typeName)
        val isUnresolved = !isPrimitive && type.reference?.resolve() == null

        if (isUnresolved) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                MCDevBundle("mixinextras.expression.lang.errors.unresolved_symbol")
            )
                .range(type)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withDefinitionFix(type)
                .create()
        } else {
            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                .range(type)
                .textAttributes(
                    if (isPrimitive) {
                        MEExpressionSyntaxHighlighter.IDENTIFIER_PRIMITIVE_TYPE
                    } else {
                        MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME
                    }
                )
                .create()
        }
    }

    private fun highlightVariable(
        holder: AnnotationHolder,
        variable: MEName,
        defaultColor: TextAttributesKey,
        isMember: Boolean,
    ) {
        val variableName = variable.text
        val isUnresolved = (variableName != "length" || !isMember) && variable.reference?.resolve() == null

        if (isUnresolved) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                MCDevBundle("mixinextras.expression.lang.errors.unresolved_symbol")
            )
                .range(variable)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .withDefinitionFix(variable)
                .create()
        } else {
            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                .range(variable)
                .textAttributes(defaultColor)
                .create()
        }
    }

    private fun AnnotationBuilder.withDefinitionFix(name: MEName) =
        withFix(AddDefinitionFix(name))

    private class AddDefinitionFix(name: MEName) : LocalQuickFixAndIntentionActionOnPsiElement(name) {
        private val id = name.text

        override fun getFamilyName(): String = MCDevBundle("mixin.expression.fix.add_definition")

        override fun getText(): String = "$familyName(id = \"$id\")"

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            if (editor == null) {
                MEExpressionCompletionUtil.addDefinition(
                    project,
                    startElement,
                    id,
                    ""
                )
                return
            }
            val annotation = MEExpressionCompletionUtil.addDefinition(
                project,
                startElement,
                id,
                "dummy = \"\""
            ) ?: return
            val dummy = annotation.parameterList.attributes.firstOrNull { it.name == "dummy" } ?: return
            val hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
            hostEditor.caretModel.moveToOffset(dummy.textOffset)
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(hostEditor.document)
            hostEditor.document.replaceString(dummy.textRange.startOffset, dummy.textRange.endOffset, "")
            AutoPopupController.getInstance(project).scheduleAutoPopup(hostEditor, null)
        }
    }
}
