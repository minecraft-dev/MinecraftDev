/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.util.Objects

/**
 * Represents a key to be used to return a data object of type T.
 */
abstract class Key<T> {
    val key: String
        get() = javaClass.canonicalName

    override fun toString(): String {
        return key
    }

    override fun hashCode(): Int {
        return Objects.hashCode(key)
    }

    override fun equals(other: Any?): Boolean {
        return other === this || other is Key<*> && other.key == key
    }
}
