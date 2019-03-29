/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

interface BukkitLikeConfiguration {
    fun hasDependencies(): Boolean
    fun setDependencies(string: String)

    fun hasSoftDependencies(): Boolean
    fun setSoftDependencies(string: String)

    val dependencies: MutableList<String>
    val softDependencies: MutableList<String>
}