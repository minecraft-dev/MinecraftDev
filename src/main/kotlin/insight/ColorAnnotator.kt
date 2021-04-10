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
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import java.awt.Color
import java.awt.Font
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType

class ColorAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!MinecraftSettings.instance.isShowChatColorUnderlines) {
            return
        }

        val color = element.toUElementOfType<UIdentifier>()?.findColor { _, chosenEntry -> chosenEntry.value } ?: return

        setColorAnnotator(color, element, holder)
    }

    companion object {
        fun setColorAnnotator(color: Color, element: PsiElement, holder: AnnotationHolder) {
            val textAttributes =
                TextAttributes(null, null, color, MinecraftSettings.instance.underlineType.effectType, Font.PLAIN)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .enforcedTextAttributes(textAttributes)
                .create()
        }
    }
}
