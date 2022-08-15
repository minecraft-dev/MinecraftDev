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
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwHeader
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwEntryMixin
import com.demonwav.mcdev.util.childrenOfType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class AwFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AwLanguage) {

    val header: AwHeader?
        get() = children.first { it is AwHeader } as? AwHeader

    val entries: Collection<AwEntryMixin>
        get() = childrenOfType()

    override fun getFileType() = AwFileType
    override fun toString() = "Access Widener File"
    override fun getIcon(flags: Int) = PlatformAssets.MCP_ICON
}
