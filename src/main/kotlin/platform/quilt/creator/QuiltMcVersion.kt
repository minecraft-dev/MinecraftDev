/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.quilt.creator

class QuiltMcVersion(
    private val ordinal: Int,
    val version: String,
    val stable: Boolean,
) : Comparable<QuiltMcVersion> {
    override fun toString() = version
    override fun compareTo(other: QuiltMcVersion) = ordinal.compareTo(other.ordinal)
}
