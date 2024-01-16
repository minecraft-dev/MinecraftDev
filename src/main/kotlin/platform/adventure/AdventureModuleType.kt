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

package com.demonwav.mcdev.platform.adventure

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.CommonColors
import javax.swing.Icon

object AdventureModuleType : AbstractModuleType<AdventureModule>(
    AdventureConstants.GROUP_ID,
    AdventureConstants.API_ARTIFACT_ID,
) {

    const val ID = "ADVENTURE_MODULE_TYPE"

    override val id = ID
    override val platformType = PlatformType.ADVENTURE

    override val icon: Icon = PlatformAssets.ADVENTURE_ICON
    override val isIconSecondary = true

    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()

    init {
        CommonColors.applyStandardColors(colorMap, AdventureConstants.NAMED_TEXT_COLOR_CLASS)
    }

    override fun generateModule(facet: MinecraftFacet) = AdventureModule(facet)
}
