/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.util.stream.Stream

/**
 * Filters all `null` elements from the [Stream].
 */
@Suppress("UNCHECKED_CAST")
fun <T> Stream<T?>.filterNotNull(): Stream<T> = filter({ it != null }) as Stream<T>

/**
 * Maps all elements with the specified mapper function and excludes all
 * returned `null` elements from the [Stream].
 */
fun <T, R> Stream<T>.mapNotNull(mapper: (T) -> R?): Stream<R> = map(mapper).filterNotNull()
