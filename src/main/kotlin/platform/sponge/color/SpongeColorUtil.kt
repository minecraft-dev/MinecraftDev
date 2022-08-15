/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
            "com.flowpowered.math.vector.Vector3i"
        )
    )
