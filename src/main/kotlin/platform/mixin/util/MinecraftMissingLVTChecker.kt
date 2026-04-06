/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.mcVersion
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement

class MinecraftMissingLVTChecker : MissingLVTChecker {
    override fun hasMissingLVT(context: PsiElement, className: String): Boolean {
        return when {
            Registry.`is`("mcdev.unobfuscated.minecraft") -> false
            !className.startsWith("net.minecraft.") && !className.startsWith("com.mojang.blaze3d.") -> false
            else -> {
                val mcVersion = context.mcVersion
                mcVersion != null && mcVersion <= MinecraftVersions.MC1_21_11
            }
        }
    }
}
