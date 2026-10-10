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

package com.demonwav.mcdev.platform.mixin.util

import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes

enum class TypeKind {
    OBJECT,
    INT_LIKE,
    FLOAT,
    DOUBLE,
    LONG,
    VOID;

    companion object {
        private val typeMap = mapOf(
            PsiTypes.byteType() to INT_LIKE,
            PsiTypes.charType() to INT_LIKE,
            PsiTypes.intType() to INT_LIKE,
            PsiTypes.shortType() to INT_LIKE,
            PsiTypes.booleanType() to INT_LIKE,
            PsiTypes.doubleType() to DOUBLE,
            PsiTypes.floatType() to FLOAT,
            PsiTypes.longType() to LONG,
            PsiTypes.voidType() to VOID,
        )

        fun of(type: PsiType) = typeMap[type] ?: OBJECT
    }
}
