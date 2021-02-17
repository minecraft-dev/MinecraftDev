/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
        TomlElementTypes.MULTILINE_BASIC_STRING, TomlElementTypes.MULTILINE_LITERAL_STRING -> TomlValueType.StringType
        TomlElementTypes.DATE_TIME -> TomlValueType.DateType
        else -> null
    }

sealed class TomlValueType(val presentableName: String) {
    object BooleanType : TomlValueType("boolean")
    object NumberType : TomlValueType("number")
    object StringType : TomlValueType("string")
    object DateType : TomlValueType("date")
}
