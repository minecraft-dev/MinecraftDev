/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.util

import com.demonwav.mcdev.util.CommonColors
import java.awt.Color

object CanaryLegacyColors {

    fun applyLegacyColors(map: MutableMap<String, Color>, prefix: String) {
        map["$prefix.RED"] = CommonColors.DARK_RED
        map["$prefix.LIGHT_RED"] = CommonColors.RED
        map["$prefix.ORANGE"] = CommonColors.GOLD
        map["$prefix.YELLOW"] = CommonColors.YELLOW
        map["$prefix.GREEN"] = CommonColors.DARK_GREEN
        map["$prefix.LIGHT_GREEN"] = CommonColors.GREEN
        map["$prefix.CYAN"] = CommonColors.AQUA
        map["$prefix.TURQUIOSE"] = CommonColors.DARK_AQUA
        map["$prefix.DARK_BLUE"] = CommonColors.DARK_BLUE
        map["$prefix.BLUE"] = CommonColors.BLUE
        map["$prefix.PINK"] = CommonColors.LIGHT_PURPLE
        map["$prefix.PURPLE"] = CommonColors.DARK_PURPLE
        map["$prefix.WHITE"] = CommonColors.WHITE
        map["$prefix.LIGHT_GRAY"] = CommonColors.GRAY
        map["$prefix.GRAY"] = CommonColors.DARK_GRAY
        map["$prefix.BLACK"] = CommonColors.BLACK
    }
}
