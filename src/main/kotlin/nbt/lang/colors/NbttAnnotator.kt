/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByte
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttDouble
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttFloat
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLong
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttShort
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttString
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTagName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class NbttAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        annotateNames(element, holder)
        annotateMaterials(element, holder)
        annotateTypes(element, holder)
    }

    private fun annotateNames(element: PsiElement, holder: AnnotationHolder) {
        if (element !is NbttTagName) {
            return
        }

        val attributes = if (!element.text.startsWith('"')) {
            NbttSyntaxHighlighter.UNQUOTED_STRING_NAME
        } else {
            NbttSyntaxHighlighter.STRING_NAME
        }

        holder.newSilentAnnotation(INFORMATION)
            .range(element)
            .textAttributes(attributes)
            .create()
    }

    private fun annotateMaterials(element: PsiElement, holder: AnnotationHolder) {
        if (element !is NbttString) {
            return
        }

        val value = element.getStringValue()
        val index = value.indexOf(':')
        if (index != -1 && !value.startsWith(':') && !value.endsWith(':') && value.count { it == ':' } == 1) {
            // assume material
            // assume this string is quoted, since the lexer won't accept an unquoted string with a : character in it
            // won't even let you escape them

            val range = TextRange(element.textRange.startOffset + index + 2, element.textRange.endOffset - 1)
            holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.material"))
                .range(range)
                .textAttributes(NbttSyntaxHighlighter.MATERIAL)
                .create()
        }
    }

    private fun annotateTypes(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is NbttByte -> {
                holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.type_byte")).range(element).create()
            }
            is NbttShort -> {
                holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.type_short")).range(element).create()
            }
            is NbttLong -> {
                holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.type_long")).range(element).create()
            }
            is NbttFloat -> {
                holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.type_float")).range(element).create()
            }
            is NbttDouble -> {
                holder.newAnnotation(INFORMATION, MCDevBundle("nbt.lang.annotate.type_double")).range(element).create()
            }
        }
    }
}
