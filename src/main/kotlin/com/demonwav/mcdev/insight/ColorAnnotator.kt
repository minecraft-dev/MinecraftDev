/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.MinecraftSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import java.awt.Color
import java.awt.Font

class ColorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!MinecraftSettings.instance.isShowChatColorUnderlines) {
            return
        }

        val color = element.findColor { _, chosenEntry -> chosenEntry.value } ?: return

        setColorAnnotator(color, element, holder)
    }

    companion object {
        fun setColorAnnotator(color: Color, element: PsiElement, holder: AnnotationHolder) {
            @Suppress("DEPRECATION")
            val key = TextAttributesKey.createTextAttributesKey("MC_COLOR_" + color.toString(), TextAttributes(
                null,
                null,
                color,
                MinecraftSettings.instance.underlineType.effectType,
                Font.PLAIN
            ))

            val annotation = holder.createAnnotation(HighlightSeverity.INFORMATION, element.textRange, null)
            annotation.textAttributes = key
        }
    }
}
