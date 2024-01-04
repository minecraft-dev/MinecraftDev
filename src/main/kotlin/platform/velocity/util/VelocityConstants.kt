/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
