/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.MinecraftSettings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
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
            val annotation = holder.createInfoAnnotation(element, null)
            annotation.enforcedTextAttributes =
                TextAttributes(null, null, color, MinecraftSettings.instance.underlineType.effectType, Font.PLAIN)
        }
    }
}
