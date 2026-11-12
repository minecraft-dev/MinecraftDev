/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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


package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.formatting.CustomFormattingModelBuilder
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelEx
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageFormatting
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Formats annotater Javadoc editors with Java's formatter, but only changes whitespace outside the guarded blocks, so
 * that the read-only copied source code is left as it is.
 */
class AnnotaterJavadocFormattingModelBuilder : CustomFormattingModelBuilder {
    override fun isEngagedToFormat(context: PsiElement) = AnnotaterJavadocVirtualFile.of(context.containingFile) != null

    override fun createModel(formattingContext: FormattingContext): FormattingModel =
        EditableRangeFormattingModel(javaBuilder().createModel(formattingContext), formattingContext.containingFile)

    override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? =
        javaBuilder().getRangeAffectingIndent(file, offset, elementAtOffset)

    private fun javaBuilder(): FormattingModelBuilder =
        LanguageFormatting.INSTANCE.allForLanguage(JavaLanguage.INSTANCE).first { it !is CustomFormattingModelBuilder }

    /**
     * The formatter shifts the following offsets by how much the returned range's length changed, so returning the
     * original range leaves the whitespace unchanged.
     */
    private class EditableRangeFormattingModel(
        private val delegate: FormattingModel,
        file: PsiFile,
    ) : FormattingModelEx, FormattingModel by delegate {
        private val document = file.viewProvider.document

        private fun isEditable(range: TextRange) = document.getRangeGuard(range.startOffset, range.endOffset) == null

        override fun replaceWhiteSpace(textRange: TextRange, whiteSpace: String): TextRange =
            if (isEditable(textRange)) {
                delegate.replaceWhiteSpace(textRange, whiteSpace)
            } else {
                textRange
            }

        override fun replaceWhiteSpace(textRange: TextRange, nodeAfter: ASTNode?, whiteSpace: String): TextRange =
            when {
                !isEditable(textRange) -> textRange
                delegate is FormattingModelEx -> delegate.replaceWhiteSpace(textRange, nodeAfter, whiteSpace)
                else -> delegate.replaceWhiteSpace(textRange, whiteSpace)
            }

        override fun shiftIndentInsideRange(node: ASTNode?, range: TextRange, indent: Int): TextRange =
            if (isEditable(range)) {
                delegate.shiftIndentInsideRange(node, range, indent)
            } else {
                range
            }
    }
}
