/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.ManifestLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class CanaryPresentationProvider : ManifestLibraryPresentationProvider(CANARY_LIBRARY_KIND, "CanaryLib") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.CANARY_ICON
}

class NeptunePresentationProvider : ManifestLibraryPresentationProvider(NEPTUNE_LIBRARY_KIND, "NeptuneLib") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.NEPTUNE_ICON
}
