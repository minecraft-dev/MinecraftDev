/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

import kotlin.reflect.KClass

val <T : Any> KClass<T>.asPrimitiveType: Class<T>
    get() = this.javaPrimitiveType ?: error("javaPrimitiveType is not available for $this")

fun Any.findDeclaredField(name: String, owner: Class<*> = javaClass): Any? {
    return runCatching {
        val field = owner.getDeclaredField(name)
        field.isAccessible = true
        field.get(this)
    }.getOrNull()
}

fun Any.invokeDeclaredMethod(
    name: String,
    params: Array<Class<*>>,
    args: Array<Any?>,
    owner: Class<*> = javaClass,
): Any? {
    return runCatching {
        val method = owner.getDeclaredMethod(name, *params)
        method.isAccessible = true
        method(this, *args)
    }.getOrNull()
}
