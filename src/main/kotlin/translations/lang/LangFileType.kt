/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.LanguageFileType

object LangFileType : LanguageFileType(MCLangLanguage) {
    private const val FILE_EXTENSION = "lang"

    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getName() = "MCLang"
    override fun getDefaultExtension() = FILE_EXTENSION
    override fun getDescription() = "Minecraft localization"
}
