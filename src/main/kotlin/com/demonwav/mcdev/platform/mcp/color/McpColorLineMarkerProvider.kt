/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.NavigateAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.intellij.ui.ColorChooser
import com.intellij.util.Function
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.TwoColorsIcon
import java.awt.Color
import javax.swing.Icon

class McpColorLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        for (element in elements) {
            val calls = element.findColors()

            for (call in calls) {
                val info = McpColorInfo(element, call)
                NavigateAction.setNavigateAction(info, "Change color", null)
                result.add(info)
            }
        }
    }

    private class McpColorInfo(private val parent: PsiElement, private val result: McpColorResult<Color>) : MergeableLineMarkerInfo<PsiElement>(
        result.expression,
        result.argRange,
        ColorIcon(12, result.arg),
        Pass.UPDATE_ALL,
        Function { result.param.description },
        GutterIconNavigationHandler handler@{ _, _ ->
            if (!result.expression.isWritable) {
                return@handler
            }

            val editor = PsiUtilBase.findEditor(result.expression) ?: return@handler

            val c = ColorChooser.chooseColor(editor.component, "Choose ${result.param.description}", result.arg, result.param.hasAlpha)
            if (c != null) {
                result.param.setColor(result.withArg(c))
            }
        },
        GutterIconRenderer.Alignment.RIGHT
    ) {
        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is McpColorInfo && info.parent == parent
        override fun getCommonIconAlignment(infos: List<MergeableLineMarkerInfo<*>>) = GutterIconRenderer.Alignment.RIGHT

        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<*>>): Icon {
            if (infos.size == 2 && infos[0] is McpColorInfo && infos[1] is McpColorInfo) {
                return TwoColorsIcon(12, (infos[0] as McpColorInfo).result.arg, (infos[1] as McpColorInfo).result.arg)
            }
            return AllIcons.Gutter.Colors
        }

        override fun getElementPresentation(element: PsiElement?): String {
            return result.param.description
        }
    }
}
