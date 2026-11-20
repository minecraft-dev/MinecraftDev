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

/**
 * Validation of JVM descriptors appearing in unpick files. Valid descriptors can be parsed with ASM's
 * [org.objectweb.asm.Type].
 */
object UnpickDescriptors {
    private const val FIELD_DESCRIPTOR = "\\[*(?:[BCDFIJSZ]|L[^.;\\[()]+;)"
    private val FIELD_DESCRIPTOR_REGEX = Regex(FIELD_DESCRIPTOR)
    private val METHOD_DESCRIPTOR_REGEX = Regex("\\((?:$FIELD_DESCRIPTOR)*\\)(?:V|$FIELD_DESCRIPTOR)")

    fun isValidField(descriptor: String) = FIELD_DESCRIPTOR_REGEX.matches(descriptor)

    fun isValidMethod(descriptor: String) = METHOD_DESCRIPTOR_REGEX.matches(descriptor)
}
