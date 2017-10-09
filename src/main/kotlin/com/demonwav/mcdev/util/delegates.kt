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

import kotlin.reflect.KProperty

private object Lock

class NullableDelegate<T>(private val supplier: () -> T?) {
    private var field: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        if (field != null) {
            return field
        }

        synchronized(Lock) {
            if (field != null) {
                return field
            }

            field = supplier()
        }

        return field
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.field = value
    }
}

fun <T> nullable(supplier: () -> T?): NullableDelegate<T>  = NullableDelegate(supplier)
