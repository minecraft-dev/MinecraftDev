/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.ManifestLibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider

class LiteLoaderPresentationProvider : ManifestLibraryPresentationProvider(LITELOADER_LIBRARY_KIND, "LiteLoader") {
    override fun getIcon(properties: LibraryPresentationProvider?) = PlatformAssets.LITELOADER_ICON
}
