/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
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

