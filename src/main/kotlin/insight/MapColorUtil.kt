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

package com.demonwav.mcdev.insight

import com.intellij.psi.PsiElement
import java.awt.Color
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType

fun PsiElement.findMapColor(): Pair<Color, UElement>? {
    val identifier = this.toUElementOfType<UIdentifier>()
        ?: return null

    val call = identifier.getParentOfType<UCallExpression>()
        ?: return null

    if (call.resolve()?.containingClass?.qualifiedName != "net.minecraft.world.level.material.MapColor") {
        return null
    }

    val params = call.valueArguments
    val argb = (params.getOrNull(1)?.evaluate() as? Number)?.toInt() ?: return null
    @Suppress("UseJBColor")
    return Color(argb, false) to call
}

