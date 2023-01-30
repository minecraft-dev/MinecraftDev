/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.data

enum class LoadOrder(private val myName: String) {
    STARTUP("Startup"),
    POSTWORLD("Post World");

    override fun toString() = myName
}
