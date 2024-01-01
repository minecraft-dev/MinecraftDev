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

package com.demonwav.mcdev.platform.neoforge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.neoforge.util.NeoForgeConstants
import com.intellij.psi.PsiClass

object NeoForgeModuleType : AbstractModuleType<NeoForgeModule>("", "") {

    private const val ID = "NEOFORGE_MODULE_TYPE"

    private val IGNORED_ANNOTATIONS = listOf(
        NeoForgeConstants.MOD_ANNOTATION,
        NeoForgeConstants.SUBSCRIBE_EVENT,
        NeoForgeConstants.EVENT_BUS_SUBSCRIBER,
    )
    private val LISTENER_ANNOTATIONS = listOf(
        NeoForgeConstants.SUBSCRIBE_EVENT,
    )

    override val platformType = PlatformType.NEOFORGE
    override val icon = PlatformAssets.NEOFORGE_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet) = NeoForgeModule(facet)
    override fun getDefaultListenerName(psiClass: PsiClass): String = defaultNameForSubClassEvents(psiClass)
}
