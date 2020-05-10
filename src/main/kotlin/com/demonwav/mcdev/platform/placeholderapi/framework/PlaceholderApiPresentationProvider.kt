package com.demonwav.mcdev.platform.placeholderapi.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.ManifestLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class PlaceholderApiPresentationProvider :
    ManifestLibraryPresentationProvider(PLACEHOLDERAPI_LIBRARY_KIND, "PlaceholderAPI", true) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.PLACEHOLDERAPI_ICON
}
