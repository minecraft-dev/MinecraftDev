/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.ManifestLibraryPresentationProvider
import com.intellij.openapi.roots.libraries.LibraryProperties

class CanaryPresentationProvider : ManifestLibraryPresentationProvider(CANARY_LIBRARY_KIND, "CanaryLib") {
    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.CANARY_ICON
}

class NeptunePresentationProvider : ManifestLibraryPresentationProvider(NEPTUNE_LIBRARY_KIND, "NeptuneLib") {
    override fun getIcon(properties: LibraryProperties<*>?) = PlatformAssets.NEPTUNE_ICON
}
