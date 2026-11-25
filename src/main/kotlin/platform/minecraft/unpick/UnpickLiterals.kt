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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.Nls

/**
 * Parsing of unpick literals, following the behavior of the reference implementation.
 */
object UnpickLiterals {
    sealed interface Result {
        data class Value(val value: Any) : Result
        data class Error(@Nls val message: String) : Result
    }

    /**
     * Parses the literal with the given token type and text. If [negated] is true, the literal is the direct operand
     * of a unary minus, which allows it to be one larger than the maximum positive value of its type. As with the
     * reference implementation, the returned value is not negated (although it may overflow).
     */
    fun parse(tokenType: IElementType, text: String, negated: Boolean): Result {
        return when (tokenType) {
            UnpickTypes.INTEGER -> parseInt(text, negated)
            UnpickTypes.LONG -> parseLong(text.dropLast(1), negated)
            UnpickTypes.FLOAT -> parseFloat(text.dropLast(1))
            UnpickTypes.DOUBLE -> parseDouble(text)
            UnpickTypes.CHAR -> parseChar(text)
            UnpickTypes.STRING -> parseString(text)
            else -> Result.Error(MCDevBundle("minecraft.unpick.literal.malformed_number"))
        }
    }

    fun parseInt(text: String): Int? = (parseInt(text, false) as? Result.Value)?.value as? Int

    fun dataTypeOf(tokenType: IElementType): DataType? {
        return when (tokenType) {
            UnpickTypes.INTEGER -> DataType.INT
            UnpickTypes.LONG -> DataType.LONG
            UnpickTypes.FLOAT -> DataType.FLOAT
            UnpickTypes.DOUBLE -> DataType.DOUBLE
            UnpickTypes.CHAR -> DataType.CHAR
            UnpickTypes.STRING -> DataType.STRING
            else -> null
        }
    }

    private data class NumberWithRadix(val digits: String, val radix: Int)

    private fun splitRadix(text: String): NumberWithRadix {
        return when {
            text.startsWith("0x") || text.startsWith("0X") -> NumberWithRadix(text.substring(2), 16)
            text.startsWith("0b") || text.startsWith("0B") -> NumberWithRadix(text.substring(2), 2)
            text.startsWith("0") && text.length > 1 -> NumberWithRadix(text.substring(1), 8)
            else -> NumberWithRadix(text, 10)
        }
    }

    private fun parseInt(text: String, negated: Boolean): Result {
        val (digits, radix) = splitRadix(text)
        val signed = (if (negated) "-$digits" else digits).toIntOrNull(radix)
        if (signed != null) {
            return Result.Value(if (negated) -signed else signed)
        }
        // Non-decimal literals may use all the bits of the type, like in Java
        if (!negated && radix != 10) {
            digits.toUIntOrNull(radix)?.let { return Result.Value(it.toInt()) }
        }
        return Result.Error(MCDevBundle("minecraft.unpick.literal.integer_out_of_bounds"))
    }

    private fun parseLong(text: String, negated: Boolean): Result {
        val (digits, radix) = splitRadix(text)
        val signed = (if (negated) "-$digits" else digits).toLongOrNull(radix)
        if (signed != null) {
            return Result.Value(if (negated) -signed else signed)
        }
        if (!negated && radix != 10) {
            digits.toULongOrNull(radix)?.let { return Result.Value(it.toLong()) }
        }
        return Result.Error(MCDevBundle("minecraft.unpick.literal.long_out_of_bounds"))
    }

    private fun parseFloat(text: String): Result {
        val value = text.toFloatOrNull()
            ?: return Result.Error(MCDevBundle("minecraft.unpick.literal.malformed_number"))
        if (!value.isFinite()) {
            return Result.Error(MCDevBundle("minecraft.unpick.literal.float_out_of_bounds"))
        }
        return Result.Value(value)
    }

    private fun parseDouble(text: String): Result {
        val value = text.toDoubleOrNull()
            ?: return Result.Error(MCDevBundle("minecraft.unpick.literal.malformed_number"))
        if (!value.isFinite()) {
            return Result.Error(MCDevBundle("minecraft.unpick.literal.double_out_of_bounds"))
        }
        return Result.Value(value)
    }

    private fun parseChar(text: String): Result {
        if (text.length < 2 || !isTerminated(text)) {
            return Result.Error(MCDevBundle("minecraft.unpick.literal.unterminated_char"))
        }
        val unquoted = unquote(text) ?: return Result.Error(MCDevBundle("minecraft.unpick.literal.illegal_escape"))
        return when (unquoted.length) {
            0 -> Result.Error(MCDevBundle("minecraft.unpick.literal.empty_char"))
            1 -> Result.Value(unquoted[0])
            else -> Result.Error(MCDevBundle("minecraft.unpick.literal.char_too_long"))
        }
    }

    private fun parseString(text: String): Result {
        if (text.length < 2 || !isTerminated(text)) {
            return Result.Error(MCDevBundle("minecraft.unpick.literal.unterminated_string"))
        }
        val unquoted = unquote(text) ?: return Result.Error(MCDevBundle("minecraft.unpick.literal.illegal_escape"))
        return Result.Value(unquoted)
    }

    private fun isTerminated(text: String): Boolean {
        // The lexer only allows the quote character to appear at the end, unless it's escaped
        val quote = text[0]
        if (text.last() != quote) {
            return false
        }
        var i = 1
        while (i < text.length - 1) {
            if (text[i] == '\\') {
                i++
            }
            i++
        }
        return i == text.length - 1
    }

    /**
     * Removes the quotes from the given literal and processes escape sequences. Returns `null` if there is an illegal
     * escape sequence. Unicode escapes are treated as escape sequences rather than being preprocessed like in Java.
     */
    private fun unquote(text: String): String? {
        val result = StringBuilder(text.length - 2)
        if (!CodeInsightUtilCore.parseStringCharacters(text.substring(1, text.length - 1), result, null)) {
            return null
        }
        return result.toString()
    }
}
