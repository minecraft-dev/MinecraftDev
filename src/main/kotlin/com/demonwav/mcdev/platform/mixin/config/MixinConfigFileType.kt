/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType

object MixinConfigFileType : LanguageFileType(JsonLanguage.INSTANCE) {
    override fun getName() = "Mixin Configuration"
    override fun getDescription() = "Mixin Configuration"
    override fun getDefaultExtension() = ""
    override fun getIcon() = PlatformAssets.MIXIN_ICON
}
