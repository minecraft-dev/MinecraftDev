/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.color

import com.demonwav.mcdev.insight.ColorLineMarkerProvider
import com.demonwav.mcdev.insight.setColor
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.NavigateAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.ColorChooser
import java.awt.Color

class SpongeColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val pair = element.findColor() ?: return null

        val info = SpongeColorInfo(element, pair.first, pair.second)
        NavigateAction.setNavigateAction(info, "Change color", null)

        return info
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<*>>) {}

    private class SpongeColorInfo(
        element: PsiElement,
        color: Color,
        workElement: PsiElement
    ) : ColorLineMarkerProvider.ColorInfo(
        element,
        color,
        GutterIconNavigationHandler handler@ { _, _ ->
        if (!element.isWritable) {
            return@handler
        }

        val editor = PsiUtilBase.findEditor(element) ?: return@handler

        val c = ColorChooser.chooseColor(editor.component, "Choose Color", color, false)
        if (c != null) {
            if (workElement is PsiLiteralExpression) {
                workElement.setColor(c.rgb and 0xFFFFFF)
            } else if (workElement is PsiExpressionList) {
                workElement.setColor(c.red, c.green, c.blue)
            } else if (workElement is PsiNewExpression) {
                (workElement.getNode().findChildByType(JavaElementType.EXPRESSION_LIST) as PsiExpressionList?)
                    ?.setColor(c.red, c.green, c.blue)
            }
        }
    })
}
