/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.creator.custom.model.TemplateApi
import com.intellij.openapi.projectRoots.JavaSdkVersion

@TemplateApi
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
    val MC1_21_1 = SemanticVersion.release(1, 21, 1)
    val MC1_21_3 = SemanticVersion.release(1, 21, 3)
    val MC1_21_5 = SemanticVersion.release(1, 21, 5)
    val MC1_21_6 = SemanticVersion.release(1, 21, 6)
    val MC1_21_8 = SemanticVersion.release(1, 21, 8)
    val MC1_21_9 = SemanticVersion.release(1, 21, 9)
    val MC1_21_10 = SemanticVersion.release(1, 21, 10)
    val MC1_21_11 = SemanticVersion.release(1, 21, 11)
    val MC26_1 = SemanticVersion.release(26, 1)
    val MC26_2 = SemanticVersion.release(26, 2)
    val MC26_3 = SemanticVersion.release(26, 3)

    fun requiredJavaVersion(minecraftVersion: SemanticVersion) = when {
        minecraftVersion <= MC1_16_5 -> JavaSdkVersion.JDK_1_8
        minecraftVersion <= MC1_17_1 -> JavaSdkVersion.JDK_16
        minecraftVersion <= MC1_20_4 -> JavaSdkVersion.JDK_17
        minecraftVersion <= MC1_21_11 -> JavaSdkVersion.JDK_21
        else -> JavaSdkVersion.JDK_25
    }

    /**
     * Returns the version of the next major Minecraft release, mimicking the code used in Forge's MDK.
     * For example 1.20.4 -> 1.21, and 26.1 -> 26.2 with the year-based versioning scheme.
     *
     * See https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
     */
    fun nextMajorVersion(minecraftVersion: SemanticVersion): String {
        val first = minecraftVersion.parts.getOrNull(0)?.versionString ?: "1"
        val mcNext = when (val part = minecraftVersion.parts.getOrNull(1)) {
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }
        return "$first.$mcNext"
    }
}
