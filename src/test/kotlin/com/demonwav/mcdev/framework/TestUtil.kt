/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("TestUtil")
package com.demonwav.mcdev.framework

fun String.toSnakeCase(postFix: String = "") =
    split("(?=[A-Z])".toRegex())
        .map(String::toLowerCase)
        .joinToString("_") + postFix
