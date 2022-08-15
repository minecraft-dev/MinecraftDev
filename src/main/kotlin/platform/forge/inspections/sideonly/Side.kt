/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.intellij.openapi.util.Key

enum class Side {

    CLIENT,
    SERVER,
    NONE,
    INVALID;

    companion object {
        val KEY = Key<Side>("MC_DEV_SIDE")
    }
}
