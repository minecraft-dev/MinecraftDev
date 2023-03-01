/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.isJavaKeyword

fun isValidClassName(className: String): Boolean {
    // default package
    if (!className.contains('.')) {
        return false
    }
    val fieldNameSplit = className.split('.')
    // crazy dots
    if (fieldNameSplit.any { it.isBlank() } || className.first() == '.' || className.last() == '.') {
        return false
    }
    // invalid character
    if (
        fieldNameSplit.any { part ->
            !part.first().isJavaIdentifierStart() ||
                !part.asSequence().drop(1).all { it.isJavaIdentifierPart() }
        }
    ) {
        return false
    }
    // keyword identifier
    return !fieldNameSplit.any { it.isJavaKeyword() }
}
