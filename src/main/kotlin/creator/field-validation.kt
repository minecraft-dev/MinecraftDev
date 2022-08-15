/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.exception.BadListSetupException
import com.demonwav.mcdev.creator.exception.EmptyInputSetupException
import com.demonwav.mcdev.creator.exception.InvalidClassNameException
import com.demonwav.mcdev.util.isJavaKeyword
import javax.swing.JTextField

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidatedField(vararg val value: ValidatedFieldType)

enum class ValidatedFieldType {
    NON_BLANK {
        override fun validate(field: JTextField) {
            if (field.text.isBlank()) {
                throw EmptyInputSetupException(field)
            }
        }
    },
    CLASS_NAME {
        override fun validate(field: JTextField) {
            if (!isValidClassName(field.text)) {
                throw InvalidClassNameException(field)
            }
        }
    },
    LIST {
        override fun validate(field: JTextField) {
            if (!field.text.matches(listPattern)) {
                throw BadListSetupException(field)
            }
        }
    };

    abstract fun validate(field: JTextField)
}

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
    if (fieldNameSplit.any { it.isJavaKeyword() }) {
        return false
    }

    return true
}

private val listPattern = Regex("""(\s*(\w+)\s*(,\s*\w+\s*)*,?|\[?\s*(\w+)\s*(,\s*\w+\s*)*])?""")
