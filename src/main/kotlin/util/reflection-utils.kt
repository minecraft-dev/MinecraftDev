/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
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
    owner: Class<*> = javaClass
): Any? {
    return runCatching {
        val method = owner.getDeclaredMethod(name, *params)
        method.isAccessible = true
        method(this, *args)
    }.getOrNull()
}
