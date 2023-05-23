/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.toml

import org.toml.lang.psi.TomlElementTypes
import org.toml.lang.psi.TomlValue
import org.toml.lang.psi.ext.elementType

fun TomlValue.stringValue(): String? {
    val delimiter = when (firstChild?.elementType) {
        TomlElementTypes.BASIC_STRING -> "\""
        TomlElementTypes.LITERAL_STRING -> "'"
        TomlElementTypes.MULTILINE_BASIC_STRING -> "\"\"\""
        TomlElementTypes.MULTILINE_LITERAL_STRING -> "'''"
        else -> return null
    }
    return text.removeSurrounding(delimiter)
}

val TomlValue.tomlType: TomlValueType?
    get() = when (firstChild?.elementType) {
        TomlElementTypes.BOOLEAN -> TomlValueType.BooleanType
        TomlElementTypes.NUMBER -> TomlValueType.NumberType
        TomlElementTypes.BASIC_STRING, TomlElementTypes.LITERAL_STRING,
        TomlElementTypes.MULTILINE_BASIC_STRING, TomlElementTypes.MULTILINE_LITERAL_STRING,
        -> TomlValueType.StringType
        TomlElementTypes.DATE_TIME -> TomlValueType.DateType
        else -> null
    }

sealed class TomlValueType(val presentableName: String) {
    object BooleanType : TomlValueType("boolean")
    object NumberType : TomlValueType("number")
    object StringType : TomlValueType("string")
    object DateType : TomlValueType("date")
}
