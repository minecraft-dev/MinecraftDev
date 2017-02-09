/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.awt.Color

object CommonColors {

    @JvmField val DARK_RED = Color(0xAA0000)
    @JvmField val RED = Color(0xFF5555)
    @JvmField val GOLD = Color(0xFFAA00)
    @JvmField val YELLOW = Color(0xFFFF55)
    @JvmField val DARK_GREEN = Color(0x00AA00)
    @JvmField val GREEN = Color(0x55FF55)
    @JvmField val AQUA = Color(0x55FFFF)
    @JvmField val DARK_AQUA = Color(0x00AAAA)
    @JvmField val DARK_BLUE = Color(0x0000AA)
    @JvmField val BLUE = Color(0x5555FF)
    @JvmField val LIGHT_PURPLE = Color(0xFF55FF)
    @JvmField val DARK_PURPLE = Color(0xAA00AA)
    @JvmField val WHITE = Color(0xFFFFFF)
    @JvmField val GRAY = Color(0xAAAAAA)
    @JvmField val DARK_GRAY = Color(0x555555)
    @JvmField val BLACK = Color(0x000000)

    @JvmStatic
    fun applyStandardColors(map: MutableMap<String, Color>, prefix: String) {
        map.apply {
            put("$prefix.DARK_RED", DARK_RED)
            put("$prefix.RED", RED)
            put("$prefix.GOLD", GOLD)
            put("$prefix.YELLOW", YELLOW)
            put("$prefix.DARK_GREEN", DARK_GREEN)
            put("$prefix.GREEN", GREEN)
            put("$prefix.AQUA", AQUA)
            put("$prefix.DARK_AQUA", DARK_AQUA)
            put("$prefix.DARK_BLUE", DARK_BLUE)
            put("$prefix.BLUE", BLUE)
            put("$prefix.LIGHT_PURPLE", LIGHT_PURPLE)
            put("$prefix.DARK_PURPLE", DARK_PURPLE)
            put("$prefix.WHITE", WHITE)
            put("$prefix.GRAY", GRAY)
            put("$prefix.DARK_GRAY", DARK_GRAY)
            put("$prefix.BLACK", BLACK)
        }
    }
}
