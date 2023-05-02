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

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings

class NbttFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val block = NbttBlock(formattingContext.node, formattingContext.codeStyleSettings, Indent.getNoneIndent(), null)
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            block,
            formattingContext.codeStyleSettings,
        )
    }

    companion object {
        fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
            val nbttSettings = settings.getCustomSettings(NbttCodeStyleSettings::class.java)
            val commonSettings = settings.getCommonSettings(NbttLanguage)

            val spacesBeforeComma = if (commonSettings.SPACE_BEFORE_COMMA) 1 else 0
            val spacesBeforeColon = if (nbttSettings.SPACE_BEFORE_COLON) 1 else 0
            val spacesAfterColon = if (nbttSettings.SPACE_AFTER_COLON) 1 else 0

            return SpacingBuilder(settings, NbttLanguage)
                .before(NbttTypes.COLON).spacing(spacesBeforeColon, spacesBeforeColon, 0, false, 0)
                .after(NbttTypes.COLON).spacing(spacesAfterColon, spacesAfterColon, 0, false, 0)
                // Empty blocks
                .between(NbttTypes.LBRACKET, NbttTypes.RBRACKET).spacing(0, 0, 0, false, 0)
                .between(NbttTypes.LPAREN, NbttTypes.RPAREN).spacing(0, 0, 0, false, 0)
                .between(NbttTypes.LBRACE, NbttTypes.RBRACE).spacing(0, 0, 0, false, 0)
                // Non-empty blocks
                .withinPair(NbttTypes.LBRACKET, NbttTypes.RBRACKET).spaceIf(commonSettings.SPACE_WITHIN_BRACKETS, true)
                .withinPair(NbttTypes.LPAREN, NbttTypes.RPAREN).spaceIf(commonSettings.SPACE_WITHIN_PARENTHESES, true)
                .withinPair(NbttTypes.LBRACE, NbttTypes.RBRACE).spaceIf(commonSettings.SPACE_WITHIN_BRACES, true)
                .before(NbttTypes.COMMA).spacing(spacesBeforeComma, spacesBeforeComma, 0, false, 0)
                .after(NbttTypes.COMMA).spaceIf(commonSettings.SPACE_AFTER_COMMA)
        }
    }
}
