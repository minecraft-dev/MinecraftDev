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

import com.demonwav.mcdev.insight.ColorLineMarkerProvider
import com.intellij.psi.PsiElement
import java.awt.Color
import org.jetbrains.uast.UElement

class SpongeColorLineMarkerProvider : ColorLineMarkerProvider.CommonLineMarkerProvider() {
    override fun findColor(element: PsiElement): Pair<Color, UElement>? = element.findSpongeColor()
}
