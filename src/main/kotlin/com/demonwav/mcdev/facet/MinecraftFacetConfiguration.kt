/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.PlatformType
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

class MinecraftFacetConfiguration : FacetConfiguration, PersistentStateComponent<MinecraftFacetConfigurationData> {

    var facet: MinecraftFacet? = null
    private var state = MinecraftFacetConfigurationData()

    override fun createEditorTabs(editorContext: FacetEditorContext?, validatorsManager: FacetValidatorsManager?) =
        arrayOf(MinecraftFacetEditorTab(this))

    override fun getState() = state
    override fun loadState(state: MinecraftFacetConfigurationData) {
        this.state = state
    }
}

data class MinecraftFacetConfigurationData(
    @Tag("userChosenTypes")
    var userChosenTypes: MutableMap<PlatformType, Boolean> = mutableMapOf(),
    @Tag("autoDetectTypes")
    @XCollection(elementName = "platformType", valueAttributeName = "", style = XCollection.Style.v2)
    var autoDetectTypes: MutableSet<PlatformType> = mutableSetOf(),
    @Tag("forgePatcher")
    var forgePatcher: Boolean = false
)
