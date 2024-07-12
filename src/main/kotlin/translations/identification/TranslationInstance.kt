/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.translations.identification

import org.jetbrains.uast.UElement

data class TranslationInstance(
    val foldingElement: UElement?,
    val foldStart: Int,
    val referenceElement: UElement?,
    val key: Key,
    val text: String?,
    val required: Boolean,
    val allowArbitraryArgs: Boolean,
    val formattingError: FormattingError? = null,
    val superfluousVarargStart: Int = -1,
) {
    data class Key(val prefix: String, val infix: String, val suffix: String) {
        constructor(infix: String) : this("", infix, "")

        val full = (prefix + infix + suffix).trim()
    }

    companion object {
        enum class FormattingError {
            MISSING, SUPERFLUOUS
        }

        fun find(element: UElement): TranslationInstance? =
            TranslationIdentifier.INSTANCES
                .firstOrNull { it.elementClass().isAssignableFrom(element.javaClass) }
                ?.identifyUnsafe(element)
    }
}
