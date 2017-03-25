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
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.lang.annotation.Annotation
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
            val key = TextAttributesKey.createTextAttributesKey(
                "MC_COLOR_" + color.toString(),
                TextAttributes(
                    null,
                    null,
                    color,
                    MinecraftSettings.instance.underlineType.effectType,
                    Font.PLAIN
                )
            )
            // We need to reset it even though we passed it in the create method, since the TextAttributesKey's are cached, so if this
            // changes then the cached version of it still wont. We set it here to make sure it's always set properly
            key.defaultAttributes.effectType = MinecraftSettings.instance.underlineType.effectType
            val annotation = Annotation(
                element.textRange.startOffset,
                element.textRange.endOffset,
                HighlightSeverity.INFORMATION, null, null
            )
            annotation.textAttributes = key
            (holder as AnnotationHolderImpl).add(annotation)
        }
    }
}
