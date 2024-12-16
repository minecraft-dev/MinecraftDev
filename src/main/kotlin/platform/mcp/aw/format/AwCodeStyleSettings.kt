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
import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class AwCodeStyleSettings(val settings: CodeStyleSettings) : CustomCodeStyleSettings("AwCodeStyleSettings", settings) {
    @JvmField
    var SPACE_BEFORE_ENTRY_COMMENT = true

    @JvmField
    var ALIGN_ENTRY_CLASS_AND_MEMBER = true
}

class AwCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
        AwCodeStyleSettings(settings)

    override fun getConfigurableDisplayName(): @NlsContexts.ConfigurableName String? = AwLanguage.displayName

    override fun createConfigurable(
        settings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable {
        return object : CodeStyleAbstractConfigurable(settings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel {
                return AwCodeStyleSettingsConfigurable(currentSettings, settings)
            }
        }
    }
}

class AwCodeStyleSettingsConfigurable(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(AwLanguage, currentSettings, settings)

class AwLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = AwLanguage

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showCustomOption(
                AwCodeStyleSettings::class.java,
                "SPACE_BEFORE_ENTRY_COMMENT",
                "Space before entry comment",
                "Spacing and alignment"
            )
            consumer.showCustomOption(
                AwCodeStyleSettings::class.java,
                "ALIGN_ENTRY_CLASS_AND_MEMBER",
                "Align entry class name and member",
                "Spacing and alignment"
            )
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String? = """
        # Some header comment

        accessible method net/minecraft/client/Minecraft pickBlock ()V # This is an entry comment
        accessible method net/minecraft/client/Minecraft userProperties ()Lcom/mojang/authlib/minecraft/UserApiService${'$'}UserProperties;

        # Each group can be aligned independently
        accessible field net/minecraft/client/gui/screens/inventory/AbstractContainerScreen clickedSlot I
        accessible field net/minecraft/client/gui/screens/inventory/AbstractContainerScreen playerInventoryTitle Ljava/lang/String;
        extendable method net/minecraft/client/gui/screens/inventory/AbstractContainerScreen findSlot (DD)Lnet/minecraft/world/inventory/Slot;
    """.trimIndent()
}
