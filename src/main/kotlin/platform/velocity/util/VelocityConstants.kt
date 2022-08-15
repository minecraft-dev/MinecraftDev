/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.util

import com.demonwav.mcdev.util.SemanticVersion

object VelocityConstants {

    const val PLUGIN_ANNOTATION = "com.velocitypowered.api.plugin.Plugin"
    const val SUBSCRIBE_ANNOTATION = "com.velocitypowered.api.event.Subscribe"
    const val KYORI_TEXT_COLOR = "net.kyori.text.format.TextColor"

    val API_2 = SemanticVersion.release(2)
    val API_3 = SemanticVersion.release(3)
    val API_4 = SemanticVersion.release(4)
}
