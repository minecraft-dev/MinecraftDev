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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.impl.MEExpressionImpl
import com.demonwav.mcdev.platform.mixin.expression.psi.mixins.MEBinaryExpressionMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet

abstract class MEBinaryExpressionImplMixin(node: ASTNode) : MEExpressionImpl(node), MEBinaryExpressionMixin {
    override val operator get() = node.findChildByType(operatorTokens)!!.elementType

    companion object {
        private val operatorTokens = TokenSet.create(
            MEExpressionTypes.TOKEN_MULT,
            MEExpressionTypes.TOKEN_DIV,
            MEExpressionTypes.TOKEN_MOD,
            MEExpressionTypes.TOKEN_PLUS,
            MEExpressionTypes.TOKEN_MINUS,
            MEExpressionTypes.TOKEN_SHL,
            MEExpressionTypes.TOKEN_SHR,
            MEExpressionTypes.TOKEN_USHR,
            MEExpressionTypes.TOKEN_LT,
            MEExpressionTypes.TOKEN_LE,
            MEExpressionTypes.TOKEN_GT,
            MEExpressionTypes.TOKEN_GE,
            MEExpressionTypes.TOKEN_EQ,
            MEExpressionTypes.TOKEN_NE,
            MEExpressionTypes.TOKEN_BITWISE_AND,
            MEExpressionTypes.TOKEN_BITWISE_XOR,
            MEExpressionTypes.TOKEN_BITWISE_OR,
            MEExpressionTypes.TOKEN_INSTANCEOF,
        )
    }
}
