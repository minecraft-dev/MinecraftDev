/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.sponge.color

import com.demonwav.mcdev.insight.findColor
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.psi.PsiElement
import java.awt.Color
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

fun PsiElement.findSpongeColor(): Pair<Color, UElement>? =
    this.toUElementOfType<UIdentifier>()?.findColor(
        SpongeModuleType,
        "org.spongepowered.api.util.Color",
        arrayOf(
            "com.flowpowered.math.vector.Vector3d",
            "com.flowpowered.math.vector.Vector3f",
            "com.flowpowered.math.vector.Vector3i",
        ),
    )
