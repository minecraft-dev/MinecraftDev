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

package com.demonwav.mcdev.util

sealed interface Quantifier {
    fun min(ctx: Context): Int

    fun max(ctx: Context): Int

    data object Default : Quantifier {
        override fun min(ctx: Context) = when (ctx) {
            Context.MEMBER, Context.INSTRUCTION -> 0
        }

        override fun max(ctx: Context) = when (ctx) {
            Context.MEMBER -> 1
            Context.INSTRUCTION -> Int.MAX_VALUE
        }

        override fun toString() = ""
    }

    data class Exact(val min: Int, val max: Int) : Quantifier {
        override fun min(ctx: Context) = min

        override fun max(ctx: Context) = max

        override fun toString() = when (this) {
            Any -> "*"
            Plus -> "+"
            else -> buildString {
                append('{')
                if (min == max) {
                    append(min)
                } else {
                    if (min != 0) {
                        append(min)
                    }
                    append(',')
                    if (max != Int.MAX_VALUE) {
                        append(max)
                    }
                }
                append('}')
            }
        }
    }

    enum class Context {
        MEMBER, INSTRUCTION
    }

    companion object {
        val Any: Quantifier = Exact(0, Int.MAX_VALUE)
        val Plus: Quantifier = Exact(1, Int.MAX_VALUE)

        fun parse(stringIn: String): Quantifier? {
            val string = stringIn.trim()
            if (string.isEmpty()) {
                return Default
            }

            if (string == "*") {
                return Any
            }

            if (string == "+") {
                return Plus
            }

            if (!string.startsWith('{') || !string.endsWith('}') || string.length < 3) {
                return null // malformed
            }

            val inner = string.substring(1, string.length - 1).trim()
            if (inner.isEmpty()) {
                return null
            }

            var strMin = inner
            var strMax = inner

            val comma = inner.indexOf(',')
            if (comma > -1) {
                strMin = inner.substring(0, comma).trim()
                strMax = inner.substring(comma + 1).trim()
            }

            val min = if (strMin.isEmpty()) 0 else strMin.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val max = if (strMax.isEmpty()) Int.MAX_VALUE else strMax.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            return Exact(min, max)
        }
    }
}
