/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.MinecraftFacetEditorTab
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.Tag
import org.jdom.Element

class MinecraftFacetConfiguration : FacetConfiguration, PersistentStateComponent<MinecraftFacetConfigurationData> {

    var facet: MinecraftFacet? = null
    private var state = MinecraftFacetConfigurationData()

    override fun createEditorTabs(editorContext: FacetEditorContext?, validatorsManager: FacetValidatorsManager?): Array<FacetEditorTab> {
        return arrayOf(MinecraftFacetEditorTab(this))
    }

    override fun getState() = state
    override fun loadState(state: MinecraftFacetConfigurationData) {
        this.state = state
    }

    override fun readExternal(element: Element?) {}
    override fun writeExternal(element: Element?) {}
}

data class MinecraftFacetConfigurationData(
    @Tag("userChosenTypes")
    var userChosenTypes: MutableMap<PlatformType, Boolean> = mutableMapOf(),
    @Tag("autoDetectTypes")
    @AbstractCollection(surroundWithTag = false, elementTag = "platformType", elementValueAttribute = "")
    var autoDetectTypes: MutableSet<PlatformType> = mutableSetOf(),
    @Tag("forgePatcher")
    var forgePatcher: Boolean = false
)
