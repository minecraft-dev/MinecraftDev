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

enum class GroupFormat(val keyword: String) {
    DECIMAL("decimal"),
    HEX("hex"),
    BINARY("binary"),
    OCTAL("octal"),
    CHAR("char");

    fun isApplicableTo(dataType: DataType): Boolean {
        return when (this) {
            DECIMAL, HEX -> dataType == DataType.INT || dataType == DataType.LONG ||
                dataType == DataType.FLOAT || dataType == DataType.DOUBLE
            BINARY, OCTAL, CHAR -> dataType == DataType.INT || dataType == DataType.LONG
        }
    }

    companion object {
        private val byKeyword = entries.associateBy { it.keyword }

        fun fromKeyword(keyword: String) = byKeyword[keyword]
    }
}
