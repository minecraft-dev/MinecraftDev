/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
        CanaryLegacyColors.applyLegacyColors(colorMap, CanaryConstants.LEGACY_COLORS_CLASS)
        CanaryLegacyColors.applyLegacyColors(colorMap, CanaryConstants.LEGACY_TEXT_FORMAT_CLASS)
    }

    override fun getPlatformType() = PlatformType.NEPTUNE
    override fun getIcon() = PlatformAssets.NEPTUNE_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = CanaryModuleType.IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = CanaryModuleType.LISTENER_ANNOTATIONS
    override fun generateModule(facet: MinecraftFacet) = CanaryModule(facet, this)
    override fun isEventGenAvailable() = true
    override fun getEventGenerationPanel(chosenClass: PsiClass) = CanaryHookGenerationPanel(chosenClass)
}
