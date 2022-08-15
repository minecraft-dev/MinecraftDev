/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.tags

import java.io.DataOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class TagDouble(override val value: Double) : NbtValueTag<Double>(Double::class.java) {
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
