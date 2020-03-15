/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.LanguageFileType

object AtFileType : LanguageFileType(AtLanguage) {

    override fun getName() = "Access Transformers"
    override fun getDescription() = "Access Transformers File"
    override fun getDefaultExtension() = ""
    override fun getIcon() = PlatformAssets.MCP_ICON
}
