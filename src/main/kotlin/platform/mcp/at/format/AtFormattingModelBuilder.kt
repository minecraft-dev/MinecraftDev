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

package com.demonwav.mcdev.platform.mcp.at.format

import com.demonwav.mcdev.platform.mcp.at.AtLanguage
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings

class AtFormattingModelBuilder : FormattingModelBuilder {

    private fun createSpaceBuilder(settings: CodeStyleSettings): SpacingBuilder {
        val atSettings = settings.getCustomSettings(AtCodeStyleSettings::class.java)
        return SpacingBuilder(settings, AtLanguage)
            .between(AtTypes.ENTRY, AtTypes.COMMENT).spaceIf(atSettings.SPACE_BEFORE_ENTRY_COMMENT)
            // Removes alignment spaces if it is disabled
            .between(AtTypes.KEYWORD, AtTypes.CLASS_NAME).spaces(1)
            .between(AtTypes.CLASS_NAME, AtTypes.FIELD_NAME).spaces(1)
            .between(AtTypes.CLASS_NAME, AtTypes.FUNCTION).spaces(1)
            .between(AtTypes.CLASS_NAME, AtTypes.ASTERISK).spaces(1)
    }

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val codeStyleSettings = formattingContext.codeStyleSettings
        val rootBlock = AtBlock(
            formattingContext.node,
            null,
            null,
            createSpaceBuilder(codeStyleSettings),
            codeStyleSettings,
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
