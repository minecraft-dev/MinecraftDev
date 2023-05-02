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
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizableOptions
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.intellij.lang.annotations.Language

class NbttLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getCodeSample(settingsType: SettingsType) = SAMPLE

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        when (settingsType) {
            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showStandardOptions("RIGHT_MARGIN")
                consumer.showCustomOption(
                    NbttCodeStyleSettings::class.java,
                    "LIST_WRAPPING",
                    "Wrap list items",
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_BRACES,
                    arrayOf(
                        "Do not wrap",
                        "Wrap as needed",
                        "Wrap always",
                    ),
                    intArrayOf(
                        CommonCodeStyleSettings.DO_NOT_WRAP,
                        CommonCodeStyleSettings.WRAP_AS_NEEDED,
                        CommonCodeStyleSettings.WRAP_ALWAYS,
                    ),
                )
                consumer.showCustomOption(
                    NbttCodeStyleSettings::class.java,
                    "ARRAY_WRAPPING",
                    "Wrap array items",
                    CodeStyleSettingsCustomizableOptions.getInstance().WRAPPING_BRACES,
                    arrayOf(
                        "Do not wrap",
                        "Wrap as needed",
                        "Wrap always",
                    ),
                    intArrayOf(
                        CommonCodeStyleSettings.DO_NOT_WRAP,
                        CommonCodeStyleSettings.WRAP_AS_NEEDED,
                        CommonCodeStyleSettings.WRAP_ALWAYS,
                    ),
                )
            }

            SettingsType.SPACING_SETTINGS -> {
                consumer.showStandardOptions(
                    "SPACE_WITHIN_BRACKETS",
                    "SPACE_WITHIN_PARENTHESES",
                    "SPACE_BEFORE_COMMA",
                    "SPACE_AFTER_COMMA",
                )
                consumer.showCustomOption(
                    NbttCodeStyleSettings::class.java,
                    "SPACE_BEFORE_COLON",
                    "Space before colon",
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS,
                )
                consumer.showCustomOption(
                    NbttCodeStyleSettings::class.java,
                    "SPACE_AFTER_COLON",
                    "Space after colon",
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_AROUND_OPERATORS,
                )

                consumer.renameStandardOption("SPACE_WITHIN_BRACKETS", "List brackets")
                consumer.renameStandardOption("SPACE_WITHIN_PARENTHESES", "Array parentheses")
            }

            else -> {
            }
        }
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun getLanguage() = NbttLanguage

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        commonSettings.RIGHT_MARGIN = 150
        indentOptions.USE_TAB_CHARACTER = true
        indentOptions.TAB_SIZE = 4
        indentOptions.INDENT_SIZE = indentOptions.TAB_SIZE
        indentOptions.CONTINUATION_INDENT_SIZE = indentOptions.INDENT_SIZE
    }
}

@Language("NBTT")
private const val SAMPLE = """
"": {
	list: [
		{
			"created-on": 1264099775885L
			"name": "Compound tag #0"
		},
		{
			primitive list: [ 0B, 1B, false, true, 14B, ]
			array: ints(1, 3, 4)
			number: 1264099775885L
		},
	]
}
"""
