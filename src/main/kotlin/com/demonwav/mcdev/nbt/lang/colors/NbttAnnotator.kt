/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.colors

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByte
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttDouble
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttFloat
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttInt
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

        val annotation = holder.createAnnotation(HighlightSeverity.INFORMATION, element.textRange, null)
        annotation.textAttributes = NbttSyntaxHighlighter.STRING_NAME

        if (!element.text.startsWith("\"")) {
            // is unquoted
            annotation.textAttributes = NbttSyntaxHighlighter.UNQUOTED_STRING_NAME
        }
    }

    private fun annotateMaterials(element: PsiElement, holder: AnnotationHolder) {
        if (element !is NbttString) {
            return
        }

        val value = element.getStringValue()
        val index = value.indexOf(':')
        if (index != -1 && !value.startsWith(":") && !value.endsWith(":") && value.count { it == ':' } == 1) {
            // assume material
            // assume this string is quoted, since the lexer won't accept an unquoted string with a : character in it
            // won't even let you escape them

            val range = TextRange(element.textRange.startOffset + index + 2, element.textRange.endOffset - 1)
            val annotation = holder.createAnnotation(HighlightSeverity.INFORMATION, range, "Material")
            annotation.textAttributes = NbttSyntaxHighlighter.MATERIAL
        }
    }

    private fun annotateTypes(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is NbttByte -> holder.createInfoAnnotation(element, "byte")
            is NbttShort -> holder.createInfoAnnotation(element, "short")
            is NbttInt -> holder.createInfoAnnotation(element, "int")
            is NbttLong -> holder.createInfoAnnotation(element, "long")
            is NbttFloat -> holder.createInfoAnnotation(element, "float")
            is NbttDouble -> holder.createInfoAnnotation(element, "double")
        }
    }
}
