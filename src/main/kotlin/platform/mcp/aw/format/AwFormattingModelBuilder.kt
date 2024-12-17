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

package com.demonwav.mcdev.platform.mcp.aw.format

import com.demonwav.mcdev.platform.mcp.aw.AwLanguage
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet

class AwFormattingModelBuilder : FormattingModelBuilder {

    private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {
        val atSettings = settings.getCustomSettings(AwCodeStyleSettings::class.java)
        var targetKindTokens = TokenSet.create(AwTypes.CLASS_ELEMENT, AwTypes.METHOD_ELEMENT, AwTypes.FIELD_ELEMENT)
        var entryTokens = TokenSet.create(AwTypes.CLASS_ENTRY, AwTypes.METHOD_ENTRY, AwTypes.FIELD_ENTRY)
        return SpacingBuilder(settings, AwLanguage)
            .between(entryTokens, AwTypes.COMMENT).spaceIf(atSettings.SPACE_BEFORE_ENTRY_COMMENT)
            // Removes alignment spaces if it is disabled
            .between(AwTypes.ACCESS_ELEMENT, targetKindTokens).spaces(1)
            .between(targetKindTokens, AwTypes.CLASS_ELEMENT).spaces(1)
            .between(AwTypes.CLASS_ELEMENT, AwTypes.MEMBER_NAME).spaces(1)
            .between(AwTypes.MEMBER_NAME, AwTypes.FIELD_DESC).spaces(1)
            .between(AwTypes.MEMBER_NAME, AwTypes.METHOD_DESC).spaces(1)
    }

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        val rootBlock = AwBlock(
            formattingContext.node,
            null,
            null,
            createSpaceBuilder(codeStyleSettings),
            codeStyleSettings,
            Alignment.createAlignment(true),
            Alignment.createAlignment(true),
            Alignment.createAlignment(true),
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            rootBlock,
            codeStyleSettings
        )
    }
}
