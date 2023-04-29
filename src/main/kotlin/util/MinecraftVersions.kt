/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.projectRoots.JavaSdkVersion

object MinecraftVersions {
    val MC1_12_2 = SemanticVersion.release(1, 12, 2)
    val MC1_14_4 = SemanticVersion.release(1, 14, 4)
    val MC1_16_1 = SemanticVersion.release(1, 16, 1)
    val MC1_16_5 = SemanticVersion.release(1, 16, 5)
    val MC1_17 = SemanticVersion.release(1, 17)
    val MC1_17_1 = SemanticVersion.release(1, 17, 1)
    val MC1_18 = SemanticVersion.release(1, 18)
    val MC1_19 = SemanticVersion.release(1, 19)
    val MC1_19_3 = SemanticVersion.release(1, 19, 3)
    val MC1_19_4 = SemanticVersion.release(1, 19, 4)

    fun requiredJavaVersion(minecraftVersion: SemanticVersion) = when {
        minecraftVersion <= MC1_16_5 -> JavaSdkVersion.JDK_1_8
        minecraftVersion <= MC1_17_1 -> JavaSdkVersion.JDK_16
        else -> JavaSdkVersion.JDK_17
    }
}
