/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.util.lang.JavaVersion

object MinecraftVersions {
    val MC1_12_2 = SemanticVersion.release(1, 12, 2)
    val MC1_14_4 = SemanticVersion.release(1, 14, 4)
    val MC1_16_1 = SemanticVersion.release(1, 16, 1)
    val MC1_17 = SemanticVersion.release(1, 17)
    val MC1_18 = SemanticVersion.release(1, 18)
    val MC1_19 = SemanticVersion.release(1, 19)

    fun requiredJavaVersion(minecraftVersion: SemanticVersion) = when {
        minecraftVersion >= MC1_18 -> JavaVersion.compose(17)
        minecraftVersion >= MC1_17 -> JavaVersion.compose(16)
        else -> JavaVersion.compose(8)
    }
}
