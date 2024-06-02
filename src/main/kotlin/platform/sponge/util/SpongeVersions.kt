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

package com.demonwav.mcdev.platform.sponge.util

import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.projectRoots.JavaSdkVersion

object SpongeVersions {

    val API8 = SemanticVersion.parse("8.0.0") to JavaSdkVersion.JDK_16
    val API9 = SemanticVersion.parse("9.0.0") to JavaSdkVersion.JDK_17
    val API10 = SemanticVersion.parse("10.0.0") to JavaSdkVersion.JDK_17
    val API11 = SemanticVersion.parse("11.0.0") to JavaSdkVersion.JDK_21

    fun requiredJavaVersion(spongeApiVersion: SemanticVersion): JavaSdkVersion {
        for ((api, java) in listOf(API8, API9, API10, API11).reversed()) {
            if (spongeApiVersion >= api) {
                return java
            }
        }

        return JavaSdkVersion.JDK_17
    }
}
