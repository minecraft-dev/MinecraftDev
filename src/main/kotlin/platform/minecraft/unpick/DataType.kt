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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes

/**
 * The data types which may be named in an unpick file.
 */
enum class DataType(val keyword: String, val descriptor: String) {
    BYTE("byte", "B"),
    SHORT("short", "S"),
    INT("int", "I"),
    LONG("long", "J"),
    FLOAT("float", "F"),
    DOUBLE("double", "D"),
    CHAR("char", "C"),
    STRING("String", "Ljava/lang/String;"),
    CLASS("Class", "Ljava/lang/Class;");

    val isPrimitive: Boolean
        get() = this != STRING && this != CLASS

    val isIntegral: Boolean
        get() = this == BYTE || this == SHORT || this == INT || this == LONG || this == CHAR

    /**
     * Whether a group may be declared with this data type. There are no literals of type `byte`, `short` or `char`
     * in bytecode, so groups of those types would be useless.
     */
    val isValidGroupType: Boolean
        get() = this == INT || this == LONG || this == FLOAT || this == DOUBLE || this == STRING || this == CLASS

    /**
     * Whether this type is the same as [target], or can be converted to it by widening primitive conversion
     * (JLS 5.1.2).
     */
    fun canWidenTo(target: DataType): Boolean {
        if (this == target) {
            return true
        }
        return when (this) {
            BYTE -> target == SHORT || target == INT || target == LONG || target == FLOAT || target == DOUBLE
            SHORT, CHAR -> target == INT || target == LONG || target == FLOAT || target == DOUBLE
            INT -> target == LONG || target == FLOAT || target == DOUBLE
            LONG -> target == FLOAT || target == DOUBLE
            FLOAT -> target == DOUBLE
            DOUBLE, STRING, CLASS -> false
        }
    }

    /**
     * Whether a target of this type may be associated with a group of type [groupType], as specified by the
     * "Targets" section of the unpick spec.
     */
    fun isTargetCompatibleWith(groupType: DataType): Boolean {
        if (!isPrimitive || !groupType.isPrimitive) {
            return this == groupType
        }
        if (this == CHAR) {
            return groupType == CHAR || groupType == BYTE || groupType == SHORT || groupType == INT || groupType == LONG
        }
        if (groupType == CHAR) {
            return groupType.isTargetCompatibleWith(this)
        }
        return true
    }

    companion object {
        private val byKeyword = entries.associateBy { it.keyword }
        private val byDescriptor = entries.associateBy { it.descriptor }

        fun fromKeyword(keyword: String) = byKeyword[keyword]

        fun fromDescriptor(descriptor: String) = byDescriptor[descriptor]

        fun fromPsiType(type: PsiType): DataType? {
            return when (type) {
                PsiTypes.byteType() -> BYTE
                PsiTypes.shortType() -> SHORT
                PsiTypes.intType() -> INT
                PsiTypes.longType() -> LONG
                PsiTypes.floatType() -> FLOAT
                PsiTypes.doubleType() -> DOUBLE
                PsiTypes.charType() -> CHAR
                is PsiClassType -> when (type.rawType().canonicalText) {
                    CommonClassNames.JAVA_LANG_STRING -> STRING
                    CommonClassNames.JAVA_LANG_CLASS -> CLASS
                    else -> null
                }
                else -> null
            }
        }
    }
}
