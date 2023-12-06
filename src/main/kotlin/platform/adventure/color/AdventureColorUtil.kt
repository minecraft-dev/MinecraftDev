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

package com.demonwav.mcdev.platform.adventure.color

import com.demonwav.mcdev.insight.findColor
import com.demonwav.mcdev.platform.adventure.AdventureConstants
import com.demonwav.mcdev.platform.adventure.AdventureModuleType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypes
import java.awt.Color
import kotlin.math.roundToInt
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType

fun PsiElement.findAdventureColor(): Pair<Color, UElement>? {
    val identifier = this.toUElementOfType<UIdentifier>()
        ?: return null

    if (identifier.name == "hsvLike") {
        val call = identifier.getParentOfType<UCallExpression>()
            ?: return null

        if (call.resolve()?.containingClass?.qualifiedName != "net.kyori.adventure.util.HSVLike") {
            return null
        }

        val params = call.valueArguments
        val h = params.getOrNull(0)?.evaluate() as? Float ?: return null
        val s = params.getOrNull(1)?.evaluate() as? Float ?: return null
        val v = params.getOrNull(2)?.evaluate() as? Float ?: return null
        return Color.getHSBColor(h, s, v) to call
    }

    if (identifier.name == "lerp") {
        val call = identifier.getParentOfType<UCallExpression>()
            ?: return null

        if (call.resolve()?.containingClass?.qualifiedName != "net.kyori.adventure.text.format.TextColor") {
            return null
        }

        val params = call.valueArguments
        val t = params.getOrNull(0)?.evaluate() as? Float ?: return null
        val (a, _) = params.getOrNull(1)?.findAdventureColor() ?: return null
        val (b, _) = params.getOrNull(2)?.findAdventureColor() ?: return null
        val clampedT = t.coerceIn(0f, 1f)
        @Suppress("UseJBColor")
        val color = Color(
            (a.red + clampedT * (b.red - a.red)).roundToInt(),
            (a.green + clampedT * (b.green - a.green)).roundToInt(),
            (a.blue + clampedT * (b.blue - a.blue)).roundToInt()
        )
        return color to call
    }

    if (identifier.name == "color") {
        val call = identifier.getParentOfType<UCallExpression>()
            ?: return null

        if (call.resolve()?.containingClass?.qualifiedName != "net.kyori.adventure.text.format.TextColor") {
            return null
        }

        val params = call.valueArguments
        if (params.size == 1 && params[0].getExpressionType() != PsiTypes.intType()) {
            return params[0].findAdventureColor()
        }
    }

    return identifier.findColor(AdventureModuleType, AdventureConstants.TEXT_COLOR_CLASS, null)
        ?: identifier.findColor(AdventureModuleType, "net.kyori.adventure.util.HSVLike", null)
}

fun UExpression.findAdventureColor(): Pair<Color, UElement>? = when (this) {
    is UCallExpression -> this.methodIdentifier?.sourcePsi?.findAdventureColor()
    is UQualifiedReferenceExpression -> this.referenceNameElement?.sourcePsi?.findAdventureColor()
    else -> null
}
