/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByte
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttDouble
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttFloat
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLong
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttShort
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttString
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTagName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
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

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
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
            holder.newAnnotation(HighlightSeverity.INFORMATION, "Material")
                .range(range)
                .textAttributes(NbttSyntaxHighlighter.MATERIAL)
                .create()
        }
    }

    private fun annotateTypes(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is NbttByte -> holder.newAnnotation(HighlightSeverity.INFORMATION, "Type: byte").range(element).create()
            is NbttShort -> holder.newAnnotation(HighlightSeverity.INFORMATION, "Type: short").range(element).create()
            is NbttLong -> holder.newAnnotation(HighlightSeverity.INFORMATION, "Type: long").range(element).create()
            is NbttFloat -> holder.newAnnotation(HighlightSeverity.INFORMATION, "Type: float").range(element).create()
            is NbttDouble -> holder.newAnnotation(HighlightSeverity.INFORMATION, "Type: double").range(element).create()
        }
    }
}
