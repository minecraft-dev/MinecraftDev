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

fun Sequence<*>.allEqual() = zipWithNext().all { (a, b) -> a == b }

inline fun <S : Any, T : S> Sequence<T>.reduceFallible(operation: (acc: S, T) -> S?): S? =
    reduceOrNull<S, _> { acc, it -> operation(acc, it) ?: return null }

fun <T> Sequence<T>.singleDistinct(): T {
    val iter = iterator()
    if (!iter.hasNext()) {
        throw NoSuchElementException("Sequence is empty.")
    }
    val first = iter.next()
    for (element in iter) {
        if (first != element) {
            throw IllegalArgumentException("Sequence has more than one distinct element.")
        }
    }
    return first
}

fun <T> Sequence<T>.singleDistinctOrNull(): T? {
    val iter = iterator()
    if (!iter.hasNext()) {
        return null
    }
    val first = iter.next()
    for (element in iter) {
        if (first != element) {
            return null
        }
    }
    return first
}
