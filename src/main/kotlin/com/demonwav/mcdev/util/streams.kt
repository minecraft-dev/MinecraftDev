/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import org.jetbrains.annotations.Contract
import java.util.stream.Stream

/**
 * Filters all `null` elements from the [Stream].
 */
@Suppress("UNCHECKED_CAST")
@Contract(pure = true)
fun <T> Stream<T?>.filterNotNull(): Stream<T> = filter { it != null } as Stream<T>

/**
 * Maps all elements with the specified mapper function and excludes all
 * returned `null` elements from the [Stream].
 */
@Contract(pure = true)
inline fun <T, R> Stream<T>.mapNotNull(crossinline transform: (T) -> R?): Stream<R> = map { transform(it) }.filterNotNull()

/**
 * Creates a typed [Array] of [T] for the elements in the [Stream].
 */
@Contract(pure = true)
inline fun <reified T> Stream<out T>.toTypedArray(): Array<T> {
    return toArray { size -> arrayOfNulls<T>(size) }
}
