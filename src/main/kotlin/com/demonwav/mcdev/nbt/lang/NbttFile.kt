/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttRootCompound
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class NbttFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, NbttLanguage) {

    override fun getFileType() = NbttFileType
    override fun getIcon(flags: Int) = PlatformAssets.MINECRAFT_ICON

    fun getRootCompound() = firstChild as NbttRootCompound
}
