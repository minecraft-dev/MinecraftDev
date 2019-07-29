/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import kotlin.reflect.KProperty

class NullableDelegate<T>(supplier: () -> T?) {
    private var field: T? = null
    private var func: (() -> T?)? = supplier

    private val lock = Lock()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        var f = field
        if (f != null) {
            return f
        }

        synchronized(lock) {
            f = field
            if (f != null) {
                return f
            }

            f = func!!()

            // Don't hold on to the supplier after it's used and returned a value
            if (f != null) {
                field = f
                func = null
            }
        }

        return f
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.field = value
    }

    private class Lock
}

fun <T> nullable(supplier: () -> T?): NullableDelegate<T> = NullableDelegate(supplier)
