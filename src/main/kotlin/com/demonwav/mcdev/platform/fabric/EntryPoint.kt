/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.creator.isValidClassName

data class EntryPoint(
    val category: String,
    val type: Type,
    val className: String,
    val interfaceName: String,
    val methodName: String? = null
) {
    val reference = when (type) {
        Type.CLASS -> className
        Type.METHOD -> "$className::$methodName"
    }

    val valid by lazy { category.isNotBlank() && isValidClassName(className) && isValidClassName(interfaceName) }

    override fun toString() = "$category -> $reference implements $interfaceName"

    enum class Type {
        CLASS, METHOD
    }
}
