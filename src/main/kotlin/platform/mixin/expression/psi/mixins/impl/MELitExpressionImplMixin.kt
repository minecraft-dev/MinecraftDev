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

package com.demonwav.mcdev.platform.mixin.expression.psi.mixins.impl

import com.demonwav.mcdev.platform.mixin.expression.MESourceMatchContext
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MELitExpressionMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiUnaryExpression
import com.intellij.psi.util.PsiUtil
import com.intellij.util.IncorrectOperationException

abstract class MELitExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MELitExpressionMixin {
    override val value get() = when (node.firstChildNode.elementType) {
        MEExpressionTypes.TOKEN_NULL_LIT -> null
        MEExpressionTypes.TOKEN_MINUS -> {
            when (node.lastChildNode.elementType) {
                MEExpressionTypes.TOKEN_INT_LIT -> {
                    val text = node.lastChildNode.text
                    if (text.startsWith("0x")) {
                        "-${text.substring(2)}".toLongOrNull(16)
                    } else {
                        "-$text".toLongOrNull()
                    }
                }
                MEExpressionTypes.TOKEN_DEC_LIT -> {
                    "-${node.lastChildNode.text}".toDoubleOrNull()
                }
                else -> throw IncorrectOperationException("Invalid number literal format")
            }
        }
        MEExpressionTypes.TOKEN_BOOL_LIT -> node.chars[0] == 't'
        MEExpressionTypes.TOKEN_INT_LIT -> {
            val text = this.text
            if (text.startsWith("0x")) {
                text.substring(2).toLongOrNull(16)
            } else {
                text.toLongOrNull()
            }
        }
        MEExpressionTypes.TOKEN_DEC_LIT -> text.toDoubleOrNull()
        else -> {
            val text = this.text
            if (text.length >= 2) {
                text.substring(1, text.length - 1).replace("\\'", "'").replace("\\\\", "\\")
            } else {
                null
            }
        }
    }

    override val isNull get() = node.firstChildNode.elementType == MEExpressionTypes.TOKEN_NULL_LIT
    override val isString get() = node.firstChildNode.elementType == MEExpressionTypes.TOKEN_STRING_TERMINATOR

    override val minusToken get() = node.firstChildNode.takeIf { it.elementType == MEExpressionTypes.TOKEN_MINUS }

    override fun matchesJava(java: PsiElement, context: MESourceMatchContext): Boolean {
        return when (java) {
            is PsiLiteral -> {
                val value = this.value
                val javaValue = java.value.widened
                // MixinExtras compares floats as strings
                when (value) {
                    is Double -> javaValue is Double && value.toString() == javaValue.toString()
                    is String -> {
                        val matchesChar =
                            value.length == 1 && javaValue is Long && value.firstOrNull()?.code?.toLong() == javaValue
                        matchesChar || value == javaValue
                    }
                    else -> value == javaValue
                }
            }
            is PsiUnaryExpression -> {
                if (java.operationTokenType != JavaTokenType.MINUS) {
                    return false
                }
                val javaOperand = PsiUtil.skipParenthesizedExprDown(java.operand) ?: return false
                if (javaOperand !is PsiLiteral) {
                    return false
                }
                val value = this.value
                val javaValue = javaOperand.value.widened
                when (value) {
                    is Long -> javaValue == -value
                    is Double -> javaValue is Double && javaValue.toString() == (-value).toString()
                    else -> false
                }
            }
            else -> false
        }
    }

    override fun getInputExprs() = emptyList<MEExpression>()

    private val Any?.widened: Any? get() = when (this) {
        is Int -> toLong()
        is Float -> toDouble()
        is Char -> code.toLong()
        else -> this
    }
}
