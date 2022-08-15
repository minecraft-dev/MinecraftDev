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
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class LangFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MCLangLanguage) {
    override fun getFileType() = LangFileType
    override fun getIcon(flags: Int) = PlatformAssets.MINECRAFT_ICON
}
