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

package com.demonwav.mcdev.nbt.tags

import java.io.DataOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class TagDouble(override val value: Double) : NbtValueTag<Double>(Double::class) {
    override val payloadSize = 8
    override val typeId = NbtTypeId.DOUBLE

    override fun write(stream: DataOutputStream) {
        stream.writeDouble(value)
    }

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState): StringBuilder {
        return sb.append(FORMATTER.format(value))
    }

    companion object {
        val FORMATTER = (NumberFormat.getInstance(Locale.ROOT) as DecimalFormat).apply {
            minimumFractionDigits = 1 // Default NBTT double format always uses a fraction, like in Kotlin
            maximumFractionDigits = 20 // Should be more than enough, but let's use 20 just in case
            groupingSize = 0 // Disables thousands separator
        }
    }
}
