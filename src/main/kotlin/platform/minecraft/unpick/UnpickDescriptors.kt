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
import org.jetbrains.annotations.Nls

/**
 * Validation and parsing of JVM descriptors appearing in unpick files.
 */
object UnpickDescriptors {

    data class MethodDescriptor(val parameters: List<String>, val returnType: String)

    /**
     * Returns an error message if [descriptor] is not a valid field descriptor, otherwise `null`.
     */
    @Nls
    fun validateField(descriptor: String): String? {
        val parser = Parser(descriptor)
        parser.fieldDescriptor()?.let { return it }
        if (!parser.atEnd) {
            return MCDevBundle("minecraft.unpick.descriptor.trailing_characters")
        }
        return null
    }

    /**
     * Returns an error message if [descriptor] is not a valid method descriptor, otherwise `null`.
     */
    @Nls
    fun validateMethod(descriptor: String): String? {
        if (!descriptor.startsWith('(')) {
            return MCDevBundle("minecraft.unpick.descriptor.expected_method")
        }
        val parser = Parser(descriptor)
        parser.pos++
        while (!parser.atEnd && parser.peek() != ')') {
            parser.fieldDescriptor()?.let { return it }
        }
        if (parser.atEnd) {
            return MCDevBundle("minecraft.unpick.descriptor.unexpected_end")
        }
        parser.pos++
        if (parser.atEnd) {
            return MCDevBundle("minecraft.unpick.descriptor.unexpected_end")
        }
        if (parser.peek() == 'V') {
            parser.pos++
        } else {
            parser.fieldDescriptor()?.let { return it }
        }
        if (!parser.atEnd) {
            return MCDevBundle("minecraft.unpick.descriptor.trailing_characters")
        }
        return null
    }

    /**
     * Splits a valid method descriptor into its parameter and return type descriptors. Returns `null` if the
     * descriptor is invalid.
     */
    fun parseMethod(descriptor: String): MethodDescriptor? {
        if (validateMethod(descriptor) != null) {
            return null
        }
        val parameters = mutableListOf<String>()
        val parser = Parser(descriptor)
        parser.pos++
        while (parser.peek() != ')') {
            val start = parser.pos
            parser.fieldDescriptor()
            parameters += descriptor.substring(start, parser.pos)
        }
        return MethodDescriptor(parameters, descriptor.substring(parser.pos + 1))
    }

    /**
     * Converts a valid field descriptor, or `V`, to the name of the type as it would appear in Java source code.
     */
    fun toJavaName(descriptor: String): String {
        val dimensions = descriptor.takeWhile { it == '[' }.length
        val element = when (val elementDesc = descriptor.substring(dimensions)) {
            "B" -> "byte"
            "C" -> "char"
            "D" -> "double"
            "F" -> "float"
            "I" -> "int"
            "J" -> "long"
            "S" -> "short"
            "Z" -> "boolean"
            "V" -> "void"
            else -> elementDesc.removePrefix("L").removeSuffix(";").replace('/', '.')
        }
        return element + "[]".repeat(dimensions)
    }

    private class Parser(val text: String) {
        var pos = 0

        val atEnd: Boolean
            get() = pos >= text.length

        fun peek() = text[pos]

        /**
         * Skips over a field descriptor, returning an error message if it is invalid.
         */
        @Nls
        fun fieldDescriptor(): String? {
            while (!atEnd && peek() == '[') {
                pos++
            }
            if (atEnd) {
                return MCDevBundle("minecraft.unpick.descriptor.unexpected_end")
            }
            when (val c = peek()) {
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> pos++
                'L' -> {
                    pos++
                    val start = pos
                    while (!atEnd && peek() != ';') {
                        val nameChar = peek()
                        if (nameChar == '.' || nameChar == '[' || nameChar == '(' || nameChar == ')') {
                            return MCDevBundle("minecraft.unpick.descriptor.illegal_character", nameChar)
                        }
                        pos++
                    }
                    if (atEnd) {
                        return MCDevBundle("minecraft.unpick.descriptor.unexpected_end")
                    }
                    if (pos == start) {
                        return MCDevBundle("minecraft.unpick.descriptor.empty_class_name")
                    }
                    pos++
                }
                else -> return MCDevBundle("minecraft.unpick.descriptor.illegal_character", c)
            }
            return null
        }
    }
}
