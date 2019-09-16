/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.exception.BadListSetupException
import com.demonwav.mcdev.creator.exception.EmptyInputSetupException
import com.demonwav.mcdev.creator.exception.InvalidClassNameException
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
            // default package
            if (!field.text.contains('.')) {
                throw InvalidClassNameException(field)
            }
            val fieldNameSplit = field.text.split('.')
            // crazy dots
            if (fieldNameSplit.any { it.isBlank() } || field.text.first() == '.' || field.text.last() == '.') {
                throw InvalidClassNameException(field)
            }
            // invalid character
            if (
                fieldNameSplit.any { part ->
                    !part.first().isJavaIdentifierStart() ||
                        !part.asSequence().drop(1).all { it.isJavaIdentifierPart() }
                }
            ) {
                throw InvalidClassNameException(field)
            }
            // keyword identifier
            if (fieldNameSplit.any { javaKeywords.contains(it) }) {
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

private val listPattern = Regex("""(\s*(\w+)\s*(,\s*\w+\s*)*,?|\[?\s*(\w+)\s*(,\s*\w+\s*)*])?""")
private val javaKeywords = setOf(
    "abstract",
    "continue",
    "for",
    "new",
    "switch",
    "assert",
    "default",
    "goto",
    "package",
    "synchronized",
    "boolean",
    "do",
    "if",
    "private",
    "this",
    "break",
    "double",
    "implements",
    "protected",
    "throw",
    "byte",
    "else",
    "import",
    "public",
    "throws",
    "case",
    "enum",
    "instanceof",
    "return",
    "transient",
    "catch",
    "extends",
    "int",
    "short",
    "try",
    "char",
    "final",
    "interface",
    "static",
    "void",
    "class",
    "finally",
    "long",
    "strictfp",
    "volatile",
    "const",
    "float",
    "native",
    "super",
    "while"
)
