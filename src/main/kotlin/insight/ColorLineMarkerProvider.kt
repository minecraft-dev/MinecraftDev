/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.MinecraftSettings
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.NavigateAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.impl.source.tree.JavaElementType
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.ui.ColorChooser
import com.intellij.util.FunctionUtil
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.ColorsIcon
import java.awt.Color
import javax.swing.Icon

class ColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!MinecraftSettings.instance.isShowChatColorGutterIcons) {
            return null
        }

        val info = element.findColor { map, chosen -> ColorInfo(element, chosen.value, map) }
        if (info != null) {
            NavigateAction.setNavigateAction(info, "Change color", null)
        }

        return info
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<*>>) {}

    open class ColorInfo : MergeableLineMarkerInfo<PsiElement> {
        protected val color: Color

        constructor(element: PsiElement, color: Color, map: Map<String, Color>) : super(
            element,
            element.textRange,
            ColorIcon(12, color),
            FunctionUtil.nullConstant<Any, String>(),
            GutterIconNavigationHandler handler@{ _, psiElement ->
                if (psiElement == null || !psiElement.isWritable || !psiElement.isValid) {
                    return@handler
                }

                val editor = PsiEditorUtil.findEditor(psiElement) ?: return@handler

                val picker = ColorPicker(map, editor.component)
                val newColor = picker.showDialog()
                if (newColor != null) {
                    psiElement.setColor(newColor)
                }
            },
            GutterIconRenderer.Alignment.RIGHT
        ) {
            this.color = color
        }

        constructor(element: PsiElement, color: Color, handler: GutterIconNavigationHandler<PsiElement>) : super(
            element,
            element.textRange,
            ColorIcon(12, color),
            FunctionUtil.nullConstant<Any, String>(),
            handler,
            GutterIconRenderer.Alignment.RIGHT
        ) {
            this.color = color
        }

        override fun canMergeWith(info: MergeableLineMarkerInfo<*>) = info is ColorInfo
        override fun getCommonIconAlignment(infos: List<MergeableLineMarkerInfo<*>>) =
            GutterIconRenderer.Alignment.RIGHT

        override fun getCommonIcon(infos: List<MergeableLineMarkerInfo<*>>): Icon {
            if (infos.size == 2 && infos[0] is ColorInfo && infos[1] is ColorInfo) {
                return ColorsIcon(12, (infos[0] as ColorInfo).color, (infos[1] as ColorInfo).color)
            }
            return AllIcons.Gutter.Colors
        }

        override fun getCommonTooltip(infos: List<MergeableLineMarkerInfo<*>>) =
            FunctionUtil.nullConstant<PsiElement, String>()
    }

    class CommonColorInfo(
        element: PsiElement,
        color: Color,
        workElement: PsiElement
    ) : ColorLineMarkerProvider.ColorInfo(
        element,
        color,
        GutterIconNavigationHandler handler@{ _, psiElement ->
            if (psiElement == null || !psiElement.isValid || !workElement.isValid || !workElement.isWritable) {
                return@handler
            }

            val editor = PsiEditorUtil.findEditor(psiElement) ?: return@handler

            val c = ColorChooser.chooseColor(editor.component, "Choose Color", color, false)
            if (c != null) {
                when (workElement) {
                    is PsiLiteralExpression -> workElement.setColor(c.rgb and 0xFFFFFF)
                    is PsiExpressionList -> workElement.setColor(c.red, c.green, c.blue)
                    is PsiNewExpression -> {
                        val list = workElement.getNode().findChildByType(JavaElementType.EXPRESSION_LIST)
                            as PsiExpressionList?
                        list?.setColor(c.red, c.green, c.blue)
                    }
                }
            }
        }
    )

    abstract class CommonLineMarkerProvider : LineMarkerProvider {
        override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
            val pair = findColor(element) ?: return null

            val info = CommonColorInfo(element, pair.first, pair.second)
            NavigateAction.setNavigateAction(info, "Change color", null)

            return info
        }

        abstract fun findColor(element: PsiElement): Pair<Color, PsiElement>?

        override fun collectSlowLineMarkers(elements: List<PsiElement>, result: Collection<LineMarkerInfo<*>>) {}
    }
}
