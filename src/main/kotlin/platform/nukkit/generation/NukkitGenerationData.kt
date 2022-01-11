/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package platform.nukkit.generation

import com.demonwav.mcdev.insight.generation.GenerationData

data class NukkitGenerationData(val isIgnoreCanceled: Boolean, val eventPriority: String) : GenerationData
