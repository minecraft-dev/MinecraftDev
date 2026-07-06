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

