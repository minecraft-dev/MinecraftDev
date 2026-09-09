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

package com.demonwav.mcdev.util

inline fun <reified T> Sequence<T>.toTypedArray(): Array<T> {
    return toList().toTypedArray()
}

fun Sequence<*>.notNullToArray(): Array<Any> {
    return filterNotNull().toList().toTypedArray()
}

fun <T> Sequence<T>.filterNotNull(transform: (T) -> Any?) = this.filter { transform(it) != null }

fun <T> Sequence<T>.memoized(): Sequence<T> {
    val cache = mutableListOf<T>()
    val iterator = this.iterator()

    return sequence {
        var index = 0
        while (true) {
            if (index < cache.size) {
                yield(cache[index])
            } else if (iterator.hasNext()) {
                val next = iterator.next()
                cache.add(next)
                yield(next)
            } else {
                break
            }
            index++
        }
    }
}

fun Sequence<*>.countIsAtLeast(n: Int) = n <= 0 || drop(n - 1).any()

fun Sequence<*>.countIsLessThan(n: Int) = n > 0 && drop(n - 1).none()
