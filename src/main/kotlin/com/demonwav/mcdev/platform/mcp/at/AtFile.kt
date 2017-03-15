/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class AtFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AtLanguage) {

    override fun getFileType() = AtFileType
    override fun toString() = "Access Transformer File"
    override fun getIcon(flags: Int) = PlatformAssets.MCP_ICON
}
