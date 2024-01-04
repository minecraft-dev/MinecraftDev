/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
    private var state = MinecraftFacetConfigurationData(
        projectReimportVersion = ProjectReimporter.CURRENT_REIMPORT_VERSION
    )

    override fun createEditorTabs(editorContext: FacetEditorContext?, validatorsManager: FacetValidatorsManager?) =
        arrayOf(MinecraftFacetEditorTabV2(this))

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
    var forgePatcher: Boolean = false,
    @Tag("projectReimportVersion")
    var projectReimportVersion: Int = 0,
)
