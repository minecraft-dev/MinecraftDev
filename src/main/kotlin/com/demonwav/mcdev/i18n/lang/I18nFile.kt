/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class I18nFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, I18nLanguage) {
    override fun getFileType() = I18nFileType
    override fun getIcon(flags: Int) = PlatformAssets.MINECRAFT_ICON
}
