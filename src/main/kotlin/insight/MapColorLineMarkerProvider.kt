package com.demonwav.mcdev.insight

import com.intellij.psi.PsiElement
import java.awt.Color
import org.jetbrains.uast.UElement

class MapColorLineMarkerProvider : ColorLineMarkerProvider.CommonLineMarkerProvider() {
    override fun findColor(element:PsiElement): Pair<Color, UElement>? = element.findMapColor()
}
