/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.adventure.color

import com.demonwav.mcdev.insight.findColor
import com.demonwav.mcdev.platform.adventure.AdventureConstants
import com.demonwav.mcdev.platform.adventure.AdventureModuleType
import com.intellij.psi.PsiElement
import java.awt.Color
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

fun PsiElement.findAdventureColor(): Pair<Color, UElement>? =
    this.toUElementOfType<UIdentifier>()?.findColor(AdventureModuleType, AdventureConstants.TEXT_COLOR_CLASS, null)
