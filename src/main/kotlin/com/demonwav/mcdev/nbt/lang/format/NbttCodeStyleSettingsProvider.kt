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
import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.openapi.options.Configurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

class NbttCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings): Configurable {
        return object : CodeStyleAbstractConfigurable(settings, originalSettings, "NBT Text") {
            override fun createPanel(settings: CodeStyleSettings?): CodeStyleAbstractPanel {
                return object : TabbedLanguageCodeStylePanel(NbttLanguage, currentSettings, settings) {
                    override fun initTabs(settings: CodeStyleSettings?) {
                        addIndentOptionsTab(settings)
                        addSpacesTab(settings)
                        addBlankLinesTab(settings)
                        addWrappingAndBracesTab(settings)
                    }
                }
            }

            override fun getHelpTopic() = null
        }
    }

    override fun getConfigurableDisplayName() = NbttLanguage.displayName

    override fun createCustomSettings(settings: CodeStyleSettings) = NbttCodeStyleSettings(settings)
}
