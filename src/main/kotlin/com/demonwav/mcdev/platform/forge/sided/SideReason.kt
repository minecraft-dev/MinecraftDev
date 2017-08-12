/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.sided

enum class SideReason(private val text: String) {
    IS_CONTAINED_IN("is contained in %s"),
    INHERITS_FROM("inherits from %s"),
    IS_OF_TYPE("is of type %s"),
    CONTAINS_PARAMETER_OF("contains parameter of type %s"),
    RETURNS_VALUE_OF("returns value of type %s"),
    OVERRIDES("overrides a sided method");

    fun getText(name: String, side: String, replacement: String): String {
        return "$name has inferred side of $side because it " + String.format(text, replacement)
    }
}
