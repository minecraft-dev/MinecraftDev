/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.util

import com.demonwav.mcdev.util.CommonColors
import java.awt.Color

object CanaryLegacyColors {

    fun applyLegacyColors(map: MutableMap<String, Color>, prefix: String) {
        map.put("$prefix.RED", CommonColors.DARK_RED)
        map.put("$prefix.LIGHT_RED", CommonColors.RED)
        map.put("$prefix.ORANGE", CommonColors.GOLD)
        map.put("$prefix.YELLOW", CommonColors.YELLOW)
        map.put("$prefix.GREEN", CommonColors.DARK_GREEN)
        map.put("$prefix.LIGHT_GREEN", CommonColors.GREEN)
        map.put("$prefix.CYAN", CommonColors.AQUA)
        map.put("$prefix.TURQUIOSE", CommonColors.DARK_AQUA)
        map.put("$prefix.DARK_BLUE", CommonColors.DARK_BLUE)
        map.put("$prefix.BLUE", CommonColors.BLUE)
        map.put("$prefix.PINK", CommonColors.LIGHT_PURPLE)
        map.put("$prefix.PURPLE", CommonColors.DARK_PURPLE)
        map.put("$prefix.WHITE", CommonColors.WHITE)
        map.put("$prefix.LIGHT_GRAY", CommonColors.GRAY)
        map.put("$prefix.GRAY", CommonColors.DARK_GRAY)
        map.put("$prefix.BLACK", CommonColors.BLACK)
    }
}
