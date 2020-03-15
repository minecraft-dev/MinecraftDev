/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bungeecord.generation.BungeeCordEventGenerationPanel
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object WaterfallModuleType :
    AbstractModuleType<BungeeCordModule<WaterfallModuleType>>("io.github.waterfallmc", "waterfall-api") {

    private const val ID = "WATERFALL_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, BungeeCordConstants.CHAT_COLOR_CLASS)
    }

    override val platformType = PlatformType.WATERFALL
    override val icon = PlatformAssets.WATERFALL_ICON
    override val id = ID
    override val ignoredAnnotations = BungeeCordModuleType.IGNORED_ANNOTATIONS
    override val listenerAnnotations = BungeeCordModuleType.LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = BungeeCordModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BungeeCordEventGenerationPanel(chosenClass)
}
