/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.editor

enum class CompressionSelection(private val selectionName: String) {
    GZIP("GZipped"),
    UNCOMPRESSED("Uncompressed");
    override fun toString() = selectionName
}
