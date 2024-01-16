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

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.psi.PsiClass

object ForgeModuleType : AbstractModuleType<ForgeModule>("", "") {

    private const val ID = "FORGE_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(
        ForgeConstants.MOD_ANNOTATION,
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION,
        ForgeConstants.EVENTBUS_SUBSCRIBE_EVENT_ANNOTATION,
    )
    private val LISTENER_ANNOTATIONS = listOf(
        ForgeConstants.EVENT_HANDLER_ANNOTATION,
        ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION,
        ForgeConstants.EVENTBUS_SUBSCRIBE_EVENT_ANNOTATION,
    )

    override val platformType = PlatformType.FORGE
    override val icon = PlatformAssets.FORGE_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet) = ForgeModule(facet)
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)

    val FG23_MC_VERSION = SemanticVersion.release(1, 12)
    val FG3_MC_VERSION = SemanticVersion.release(1, 13)
    val FG3_FORGE_VERSION = SemanticVersion.release(14, 23, 5, 2851)
}
