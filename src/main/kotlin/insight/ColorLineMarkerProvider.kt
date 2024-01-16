/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.NavigateAction
import com.intellij.codeInsight.hint.HintManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.JVMElementFactories
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiEditorUtil
import com.intellij.ui.ColorChooserService
import com.intellij.util.FunctionUtil
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.ColorsIcon
import java.awt.Color
import javax.swing.Icon
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.toUElementOfType

class ColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!MinecraftSettings.instance.isShowChatColorGutterIcons) {
            return null
        }

        val identifier = element.toUElementOfType<UIdentifier>() ?: return null
        val info = runCatchingKtIdeaExceptions {
            identifier.findColor { map, chosen -> ColorInfo(element, chosen.value, map, chosen.key, identifier) }
        }
        if (info != null) {
            NavigateAction.setNavigateAction(info, MCDevBundle("generate.color.change_action"), null)
        }

        return info
    }

    open class ColorInfo : MergeableLineMarkerInfo<PsiElement> {
        protected val color: Color

        constructor(
            element: PsiElement,
            color: Color,
            map: Map<String, Color>,
            colorName: String,
            workElement: UElement,
        ) : super(
            element,
            element.textRange,
            ColorIcon(12, color),
            FunctionUtil.nullConstant<Any, String>(),
            GutterIconNavigationHandler handler@{ _, psiElement ->
                if (psiElement == null || !psiElement.isWritable || !psiElement.isValid || !workElement.isPsiValid) {
                    return@handler
                }

                val editor = PsiEditorUtil.findEditor(psiElement) ?: return@handler

                val picker = ColorPicker(map, element.project, editor.component)
                val newColor = picker.showDialog()
                if (newColor != null && map[newColor] != color) {
                    workElement.setColor(newColor)
                }
            },
            GutterIconRenderer.Alignment.RIGHT,
            { "$colorName color indicator" },
        ) {
            this.color = color
        }

        constructor(element: PsiElement, color: Color, handler: GutterIconNavigationHandler<PsiElement>) : super(
            element,
            element.textRange,
            ColorIcon(12, color),
            FunctionUtil.nullConstant<Any, String>(),
            handler,
            GutterIconRenderer.Alignment.RIGHT,
            { "color indicator" },
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
        workElement: UElement,
    ) : ColorInfo(
        element,
        color,
        GutterIconNavigationHandler handler@{ _, psiElement ->
            if (psiElement == null || !psiElement.isValid ||
                !workElement.isPsiValid || workElement.sourcePsi?.isWritable != true
            ) {
                return@handler
            }

            val editor = PsiEditorUtil.findEditor(psiElement) ?: return@handler
            if (JVMElementFactories.getFactory(psiElement.language, psiElement.project) == null) {
                // The setColor methods used here require a JVMElementFactory. Unfortunately the Kotlin plugin does not
                // implement it yet. It is better to not display the color chooser at all than deceiving users after
                // after they chose a color
                HintManager.getInstance()
                    .showErrorHint(editor, MCDevBundle("generate.color.change_error", psiElement.language.displayName))
                return@handler
            }

            val actionText = MCDevBundle("generate.color.choose_action")
            val c = ColorChooserService.instance.showDialog(psiElement.project, editor.component, actionText, color)
                ?: return@handler
            when (workElement) {
                is ULiteralExpression -> {
                    val currentValue = workElement.evaluate()
                    if (currentValue is Int) {
                        workElement.setColor(c.rgb and 0xFFFFFF)
                    } else if (currentValue is String) {
                        if (currentValue.length == 4) {
                            val hexString = "#" +
                                Integer.toUnsignedString(c.red, 16).first() +
                                Integer.toUnsignedString(c.green, 16).first() +
                                Integer.toUnsignedString(c.blue, 16).first()
                            workElement.setColor(hexString, true)
                        } else {
                            val hexString = "#" + Integer.toUnsignedString(c.rgb, 16).substring(2)
                            workElement.setColor(hexString, true)
                        }
                    }
                }

                is UCallExpression -> {
                    if (workElement.methodName == "hsvLike") {
                        val (h, s, v) = Color.RGBtoHSB(c.red, c.green, c.blue, null)
                        workElement.setColorHSV(h, s, v)
                    }

                    workElement.setColor(c.red, c.green, c.blue)
                }
            }
        },
    )

    abstract class CommonLineMarkerProvider : LineMarkerProvider {
        override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
            val pair = findColor(element) ?: return null

            val info = CommonColorInfo(element, pair.first, pair.second)
            NavigateAction.setNavigateAction(info, MCDevBundle("generate.color.change_action"), null)

            return info
        }

        abstract fun findColor(element: PsiElement): Pair<Color, UElement>?
    }
}
