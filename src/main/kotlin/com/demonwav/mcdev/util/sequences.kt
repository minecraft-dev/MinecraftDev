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

inline fun <reified T> Sequence<T>.toTypedArray(): Array<T> {
    return toList().toTypedArray()
}

fun Sequence<*>.notNullToArray(): Array<Any> {
    return filterNotNull().toList().toTypedArray()
}

fun <T> Sequence<T>.filterNotNull(transform: (T) -> Any?) = this.filter { transform(it) != null }
