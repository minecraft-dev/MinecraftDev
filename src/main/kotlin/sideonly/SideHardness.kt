/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

enum class SideHardness {
    /** Stripped at runtime or compile-time */
    HARD,
    /** Not stripped but should only be loaded on the correct side */
    SOFT,
    EITHER
}
