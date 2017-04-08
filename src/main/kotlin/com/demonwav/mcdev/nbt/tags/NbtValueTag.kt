/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.tags

abstract class NbtValueTag<T : Any>(protected val valueClass: Class<T>) : NbtTag {

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

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString() = toString(StringBuilder(), 0).toString()

    override fun toString(sb: StringBuilder, indentLevel: Int): StringBuilder {
        return sb.append(value)
    }

    override fun copy(): NbtValueTag<T> {
        val const = typeId.tagClass.java.getConstructor(valueClass)
        @Suppress("UNCHECKED_CAST")
        return const.newInstance(valueCopy()) as NbtValueTag<T>
    }

    protected open fun valueEquals(otherValue: T): Boolean {
        return this.value == otherValue
    }

    protected open fun valueCopy(): T {
        return this.value
    }
}
