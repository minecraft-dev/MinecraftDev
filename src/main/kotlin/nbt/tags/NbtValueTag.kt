/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

abstract class NbtValueTag<T : Any>(private val valueClass: Class<T>) : NbtTag {

    abstract val value: T

    override fun equals(other: Any?): Boolean {
        if (other !is NbtValueTag<*>) {
            return false
        }

        if (other === this) {
            return true
        }

        if (!this.javaClass.isAssignableFrom(other.javaClass)) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        return valueEquals(other.value as T)
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = toString(StringBuilder(), 0, WriterState.COMPOUND).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int, writerState: WriterState) = sb.append(value)!!

    override fun copy(): NbtValueTag<T> {
        val const = typeId.tagClass.java.getConstructor(valueClass)
        @Suppress("UNCHECKED_CAST")
        return const.newInstance(valueCopy()) as NbtValueTag<T>
    }

    protected open fun valueEquals(otherValue: T) = this.value == otherValue

    protected open fun valueCopy() = this.value
}
