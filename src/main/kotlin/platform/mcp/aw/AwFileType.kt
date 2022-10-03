/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.LanguageFileType

object AwFileType : LanguageFileType(AwLanguage) {

    override fun getName() = "Access Widener"

    override fun getDescription() = "Access widener"

    override fun getDefaultExtension() = "accesswidener"

    override fun getIcon() = PlatformAssets.MCP_ICON
}
