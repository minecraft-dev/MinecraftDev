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
import com.demonwav.mcdev.platform.mixin.expression.psi.MEBinaryExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MECastExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEClassConstantExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.psi.MEIdentifierAssignmentStatement
import com.demonwav.mcdev.platform.mixin.expression.psi.MEInstantiationExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MELitExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEMemberAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEMemberAssignStatement
import com.demonwav.mcdev.platform.mixin.expression.psi.MEMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MEStaticMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.psi.MESuperCallExpression
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class MEExpressionAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is MEName -> {
                if (!element.isWildcard) {
                    when (val parent = element.parent) {
                        is MECastExpression,
                        is MEClassConstantExpression,
                        is MEInstantiationExpression -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CLASS_NAME)
                            .create()
                        is MEMemberAccessExpression,
                        is MEMemberAssignStatement -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_MEMBER_NAME)
                            .create()
                        is MESuperCallExpression,
                        is MEMethodCallExpression,
                        is MEStaticMethodCallExpression -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_CALL)
                            .create()
                        is MEIdentifierAssignmentStatement -> holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                            .range(element)
                            .textAttributes(MEExpressionSyntaxHighlighter.IDENTIFIER_VARIABLE)
                            .create()
                        is MENameExpression -> {
                            val grandparent = parent.parent
                            if (grandparent is MEBinaryExpression &&
                                grandparent.operator == MEExpressionTypes.TOKEN_INSTANCEOF &&
                                grandparent.rightExpr == parent
                            ) {
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
                    holder.newAnnotation(HighlightSeverity.ERROR, MCDevBundle("mixinextras.expression.lang.errors.invalid_number"))
                        .range(element)
                        .create()
                }
            }
            is MEBinaryExpression -> {
                val rightExpr = element.rightExpr
                if (element.operator == MEExpressionTypes.TOKEN_INSTANCEOF && rightExpr !is MENameExpression && rightExpr != null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, MCDevBundle("mixinextras.expression.lang.errors.instanceof_non_type"))
                        .range(rightExpr)
                        .create()
                }
            }
        }
    }
}
