package com.demonwav.mcdev.platform.placeholderapi

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType

object PlaceholderApiModuleType : AbstractModuleType<PlaceholderApiModule>("", "") {
    private const val ID = "PLACEHOLDERAPI_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = emptyList<String>()
    val LISTENER_ANNOTATIONS = emptyList<String>()

    override val platformType = PlatformType.PLACEHOLDERAPI
    override val icon = PlatformAssets.PLACEHOLDERAPI_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = PlaceholderApiModule(facet)
}
