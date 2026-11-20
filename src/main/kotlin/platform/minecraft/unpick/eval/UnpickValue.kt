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

package com.demonwav.mcdev.platform.minecraft.unpick.eval

import com.demonwav.mcdev.platform.minecraft.unpick.DataType
import com.demonwav.mcdev.util.ClassNameResolutionFailedException
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType

/**
 * The result of evaluating an unpick expression.
 *
 * The [value] is represented as a Kotlin [Byte], [Short], [Char], [Int], [Long], [Float], [Double] or [String] for the
 * corresponding data types, as the [PsiType] referred to by a `Class` constant, or as `null` for a `null` `String` or
 * `Class` constant.
 *
 * If [isKnown] is false then only the type of the expression is known, not its value.
 */
data class UnpickValue(val type: DataType, val value: Any?, val isKnown: Boolean = true) {

    /**
     * Whether this value can be converted to [target] by [convertTo].
     */
    fun canConvertTo(target: DataType) = target == type || type.isPrimitive && target.isPrimitive

    /**
     * Converts this value to [target], as a Java cast would. Throws an [IllegalArgumentException] if the value cannot
     * be converted, which can be checked with [canConvertTo].
     */
    fun convertTo(target: DataType): UnpickValue {
        require(canConvertTo(target)) { "Cannot convert $type to $target" }
        if (target == type) {
            return this
        }
        if (!isKnown) {
            return unknown(target)
        }
        return UnpickValue(target, convert(value!!, target))
    }

    fun asLong(): Long = when (value) {
        is Char -> value.code.toLong()
        is Number -> value.toLong()
        else -> throw IllegalStateException("Not an integral value: $value")
    }

    /**
     * Converts the value to a string the same way Java string concatenation would, or returns `null` if that can't be
     * determined.
     */
    fun toJavaString(): String? {
        if (!isKnown) {
            return null
        }
        return when (value) {
            null -> "null"
            is String -> value
            is Char -> value.toString()
            is Number -> value.toString()
            is PsiType -> classToString(value)
            else -> null
        }
    }

    /**
     * A human-readable representation of the value, for use in messages.
     */
    fun toDisplayString(): String {
        return when (value) {
            null -> "null"
            is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            is Char -> "'$value'"
            is Long -> "${value}L"
            is Float -> "${value}f"
            is PsiType -> value.presentableText + ".class"
            else -> value.toString()
        }
    }

    companion object {
        fun unknown(type: DataType) = UnpickValue(type, null, isKnown = false)

        /**
         * Returns the result of calling `toString()` on the `Class` object for [type], or `null` if it can't be
         * determined.
         */
        private fun classToString(type: PsiType): String? {
            return try {
                when (type) {
                    is PsiPrimitiveType -> type.name
                    is PsiArrayType -> if ((type.deepComponentType as? PsiClassType)?.resolve() == null &&
                        type.deepComponentType !is PsiPrimitiveType
                    ) {
                        null
                    } else {
                        "class " + type.descriptor.replace('/', '.')
                    }
                    is PsiClassType -> {
                        val psiClass = type.resolve() ?: return null
                        val name = psiClass.fullQualifiedName ?: return null
                        if (psiClass.isInterface) "interface $name" else "class $name"
                    }
                    else -> null
                }
            } catch (_: ClassNameResolutionFailedException) {
                null
            }
        }

        private fun convert(value: Any, target: DataType): Any {
            if (value is Float || value is Double) {
                val d = (value as Number).toDouble()
                return when (target) {
                    DataType.BYTE -> d.toInt().toByte()
                    DataType.SHORT -> d.toInt().toShort()
                    DataType.CHAR -> d.toInt().toChar()
                    DataType.INT -> d.toInt()
                    DataType.LONG -> d.toLong()
                    DataType.FLOAT -> if (value is Float) value else d.toFloat()
                    DataType.DOUBLE -> d
                    else -> value
                }
            }
            val l = when (value) {
                is Char -> value.code.toLong()
                is Number -> value.toLong()
                else -> return value
            }
            return when (target) {
                DataType.BYTE -> l.toInt().toByte()
                DataType.SHORT -> l.toInt().toShort()
                DataType.CHAR -> l.toInt().toChar()
                DataType.INT -> l.toInt()
                DataType.LONG -> l
                DataType.FLOAT -> l.toFloat()
                DataType.DOUBLE -> l.toDouble()
                else -> value
            }
        }
    }
}
