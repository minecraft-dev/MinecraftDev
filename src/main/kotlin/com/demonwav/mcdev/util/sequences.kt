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

inline fun <reified T> Sequence<T>.toTypedArray(): Array<T> {
    return toList().toTypedArray()
}
