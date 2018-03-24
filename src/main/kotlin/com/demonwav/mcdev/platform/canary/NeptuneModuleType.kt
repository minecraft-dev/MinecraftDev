/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.canary.generation.CanaryHookGenerationPanel
import com.demonwav.mcdev.platform.canary.util.CanaryConstants
import com.demonwav.mcdev.platform.canary.util.CanaryLegacyColors
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object NeptuneModuleType : AbstractModuleType<CanaryModule<NeptuneModuleType>>("org.neptunepowered", "NeptuneLib") {

    private const val ID = "NEPTUNE_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, CanaryConstants.CHAT_FORMAT_CLASS)
        CommonColors.applyStandardColors(colorMap, CanaryConstants.MCP_CHAT_FORMATTING)
        CanaryLegacyColors.applyLegacyColors(colorMap, CanaryConstants.LEGACY_COLORS_CLASS)
        CanaryLegacyColors.applyLegacyColors(colorMap, CanaryConstants.LEGACY_TEXT_FORMAT_CLASS)
    }

    override val platformType = PlatformType.NEPTUNE
    override val icon = PlatformAssets.NEPTUNE_ICON
    override val id = ID
    override val ignoredAnnotations = CanaryModuleType.IGNORED_ANNOTATIONS
    override val listenerAnnotations = CanaryModuleType.LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet) = CanaryModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = CanaryHookGenerationPanel(chosenClass)
}
