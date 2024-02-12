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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEBinaryExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEDeclaration
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEInstantiationExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MELitExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMemberAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENewArrayExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStaticMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MESuperCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType

class MEExpressionAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MEDeclaration -> {
                val injectionHost = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element)
                    ?: return
                val declarationAnnotation = injectionHost.parentOfType<PsiAnnotation>() ?: return
                if (!declarationAnnotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                    return
                }
                if (declarationAnnotation.findDeclaredAttributeValue("type") != null) {
                    holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                        .range(element)
                        .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_TYPE_DECLARATION)
                        .create()
                } else {
                    holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                        .range(element)
                        .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_DECLARATION)
                        .create()
                }
            }
            is MEName -> {
                if (!element.isWildcard) {
                    when (val parent = element.parent) {
                        is METype,
                        is MEInstantiationExpression,
                        is MENewArrayExpression -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME)
                            .create()
                        is MEMemberAccessExpression -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_MEMBER_NAME)
                            .create()
                        is MESuperCallExpression,
                        is MEMethodCallExpression,
                        is MEStaticMethodCallExpression -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CALL)
                            .create()
                        is MENameExpression -> {
                            if (METypeUtil.isExpressionInTypePosition(parent)) {
                                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                                    .range(element)
                                    .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME)
                                    .create()
                            } else {
                                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                                    .range(element)
                                    .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_VARIABLE)
                                    .create()
                            }
                        }
                        else -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME)
                            .create()
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
                if (METypeUtil.isExpressionInTypePosition(element)) {
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
            is MENewArrayExpression -> {
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
            }
        }
    }
}
