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

import java.util.stream.Stream

/**
 * Filters all `null` elements from the [Stream].
 */
@Suppress("UNCHECKED_CAST")
fun <T> Stream<T?>.filterNotNull(): Stream<T> = filter { it != null } as Stream<T>

/**
 * Maps all elements with the specified mapper function and excludes all
 * returned `null` elements from the [Stream].
 */
inline fun <T, R> Stream<T>.mapNotNull(crossinline transform: (T) -> R?): Stream<R> =
    map { transform(it) }.filterNotNull()

/**
 * Creates a typed [Array] of [T] for the elements in the [Stream].
 */
inline fun <reified T> Stream<out T>.toTypedArray(): Array<T> {
    return toArray { size -> arrayOfNulls<T>(size) }
}
