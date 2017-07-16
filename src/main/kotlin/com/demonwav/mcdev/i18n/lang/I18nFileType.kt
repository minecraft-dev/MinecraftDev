/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.LanguageFileType

object I18nFileType : LanguageFileType(I18nLanguage) {
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getName() = "I18n"
    override fun getDefaultExtension() = "lang"
    override fun getDescription() = "Minecraft localization"
}
