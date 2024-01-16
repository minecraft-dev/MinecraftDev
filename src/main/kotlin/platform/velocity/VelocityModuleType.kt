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

package com.demonwav.mcdev.platform.velocity

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.velocity.generation.VelocityEventGenerationPanel
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object VelocityModuleType : AbstractModuleType<VelocityModule>("com.velocitypowered", "velocity") {

    private const val ID = "VELOCITY_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, VelocityConstants.KYORI_TEXT_COLOR)
    }

    override val platformType = PlatformType.VELOCITY
    override val icon = PlatformAssets.VELOCITY_ICON
    override val id = ID

    override val ignoredAnnotations =
        listOf(VelocityConstants.SUBSCRIBE_ANNOTATION, VelocityConstants.PLUGIN_ANNOTATION)
    override val listenerAnnotations =
        listOf(VelocityConstants.SUBSCRIBE_ANNOTATION)
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet): VelocityModule = VelocityModule(facet)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = VelocityEventGenerationPanel(chosenClass)
}
