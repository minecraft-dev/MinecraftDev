/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.openapi.fileTypes.LanguageFileType

object NbttFileType : LanguageFileType(NbttLanguage) {
    override fun getIcon() = PlatformAssets.MINECRAFT_ICON
    override fun getName() = "NBTT"
    override fun getDefaultExtension() = "nbtt"
    override fun getDescription() = "NBT text representation (don't use this one)"
}
