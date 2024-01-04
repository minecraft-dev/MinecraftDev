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
