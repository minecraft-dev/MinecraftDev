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
import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

class NbttCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings) =
        object : CodeStyleAbstractConfigurable(settings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings) = NbttCodeStyleMainPanel(currentSettings, settings)
            override fun getHelpTopic(): String? = null
        }

    override fun getConfigurableDisplayName() = "NBT Text"

    override fun createCustomSettings(settings: CodeStyleSettings) = NbttCodeStyleSettings(settings)

    private class NbttCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
        TabbedLanguageCodeStylePanel(NbttLanguage, currentSettings, settings) {

        override fun initTabs(settings: CodeStyleSettings?) {
            addIndentOptionsTab(settings)
            addWrappingAndBracesTab(settings)
            addSpacesTab(settings)
        }
    }
}
