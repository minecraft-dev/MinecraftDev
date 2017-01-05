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

inline fun <reified T> Stream<out T>.toTypedArray(): Array<T> {
    return toArray { size -> arrayOfNulls<T>(size) }
}
