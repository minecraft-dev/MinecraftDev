/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings

class NbttFormattingModelBuilder : FormattingModelBuilder {
    override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode) = null

    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        val block = NbttBlock(element.node, settings, Indent.getNoneIndent(), null)
        return FormattingModelProvider.createFormattingModelForPsiFile(element.containingFile, block, settings)
    }

    companion object {
        fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
            val nbttSettings = settings.getCustomSettings(NbttCodeStyleSettings::class.java)
            val commonSettings = settings.getCommonSettings(NbttLanguage)

            val spacesBeforeComma = if (commonSettings.SPACE_BEFORE_COMMA) 1 else 0
            val spacesBeforeColon = if (nbttSettings.SPACE_BEFORE_COLON) 1 else 0
            val sspacesAfterColon = if (nbttSettings.SPACE_AFTER_COLON) 1 else 0

            return SpacingBuilder(settings, NbttLanguage)
                .before(NbttTypes.COLON).spacing(spacesBeforeColon, spacesBeforeColon, 0, false, 0)
                .after(NbttTypes.COLON).spacing(sspacesAfterColon, sspacesAfterColon, 0, false, 0)
                .withinPair(NbttTypes.LBRACKET, NbttTypes.RBRACKET).spaceIf(commonSettings.SPACE_WITHIN_BRACKETS, true)
                .withinPair(NbttTypes.LPAREN, NbttTypes.RPAREN).spaceIf(commonSettings.SPACE_WITHIN_PARENTHESES, true)
                .withinPair(NbttTypes.LBRACE, NbttTypes.RBRACE).spaceIf(commonSettings.SPACE_WITHIN_BRACES, true)
                .before(NbttTypes.COMMA).spacing(spacesBeforeComma, spacesBeforeComma, 0, false, 0)
                .after(NbttTypes.COMMA).spaceIf(commonSettings.SPACE_AFTER_COMMA)
        }
    }
}
