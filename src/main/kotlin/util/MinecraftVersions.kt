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

package com.demonwav.mcdev.util

import com.intellij.openapi.projectRoots.JavaSdkVersion

object MinecraftVersions {
    val MC1_12_2 = SemanticVersion.release(1, 12, 2)
    val MC1_14_4 = SemanticVersion.release(1, 14, 4)
    val MC1_16 = SemanticVersion.release(1, 16)
    val MC1_16_1 = SemanticVersion.release(1, 16, 1)
    val MC1_16_5 = SemanticVersion.release(1, 16, 5)
    val MC1_17 = SemanticVersion.release(1, 17)
    val MC1_17_1 = SemanticVersion.release(1, 17, 1)
    val MC1_18 = SemanticVersion.release(1, 18)
    val MC1_18_2 = SemanticVersion.release(1, 18, 2)
    val MC1_19 = SemanticVersion.release(1, 19)
    val MC1_19_3 = SemanticVersion.release(1, 19, 3)
    val MC1_19_4 = SemanticVersion.release(1, 19, 4)
    val MC1_20 = SemanticVersion.release(1, 20)
    val MC1_20_1 = SemanticVersion.release(1, 20, 1)
    val MC1_20_2 = SemanticVersion.release(1, 20, 2)
    val MC1_20_3 = SemanticVersion.release(1, 20, 3)
    val MC1_20_4 = SemanticVersion.release(1, 20, 4)
    val MC1_20_5 = SemanticVersion.release(1, 20, 5)
    val MC1_20_6 = SemanticVersion.release(1, 20, 6)
    val MC1_21 = SemanticVersion.release(1, 21)

    fun requiredJavaVersion(minecraftVersion: SemanticVersion) = when {
        minecraftVersion <= MC1_16_5 -> JavaSdkVersion.JDK_1_8
        minecraftVersion <= MC1_17_1 -> JavaSdkVersion.JDK_16
        minecraftVersion <= MC1_20_4 -> JavaSdkVersion.JDK_17
        else -> JavaSdkVersion.JDK_21
    }
}
