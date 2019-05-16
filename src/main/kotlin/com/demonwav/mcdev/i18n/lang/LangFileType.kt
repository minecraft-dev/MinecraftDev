/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.i18n.I18nConstants
import com.intellij.openapi.fileTypes.LanguageFileType

object LangFileType : LanguageFileType(MCLangLanguage) {
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getName() = "MCLang"
    override fun getDefaultExtension() = I18nConstants.FILE_EXTENSION
    override fun getDescription() = "Minecraft localization"
}
