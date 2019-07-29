/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.generation

import com.demonwav.mcdev.insight.generation.GenerationData

data class BukkitGenerationData(val isIgnoreCanceled: Boolean, val eventPriority: String) : GenerationData
