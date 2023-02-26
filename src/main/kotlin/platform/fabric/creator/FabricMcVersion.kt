/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

class FabricMcVersion(
    private val ordinal: Int,
    val version: String,
    val stable: Boolean,
) : Comparable<FabricMcVersion> {
    override fun toString() = version
    override fun compareTo(other: FabricMcVersion) = ordinal.compareTo(other.ordinal)
}
