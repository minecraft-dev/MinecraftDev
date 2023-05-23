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
