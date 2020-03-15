/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.ManifestLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class LiteLoaderPresentationProvider :
    ManifestLibraryPresentationProvider(LITELOADER_LIBRARY_KIND, "LiteLoader", true) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.LITELOADER_ICON
}
