/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.generation.SpongeEventGenerationPanel
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object SpongeModuleType : AbstractModuleType<SpongeModule>("org.spongepowered", "spongeapi") {

    private const val ID = "SPONGE_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(
        SpongeConstants.LISTENER_ANNOTATION,
        SpongeConstants.PLUGIN_ANNOTATION,
        SpongeConstants.JVM_PLUGIN_ANNOTATION,
    )
    private val LISTENER_ANNOTATIONS = listOf(SpongeConstants.LISTENER_ANNOTATION)

    init {
        CommonColors.applyStandardColors(colorMap, SpongeConstants.TEXT_COLORS)
    }

    override val platformType = PlatformType.SPONGE
    override val icon = PlatformAssets.SPONGE_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet) = SpongeModule(facet)
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = SpongeEventGenerationPanel(chosenClass)
}
