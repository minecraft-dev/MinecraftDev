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

object BungeeCordModuleType : AbstractModuleType<BungeeCordModule<BungeeCordModuleType>>("net.md-5", "bungeecord-api") {

    private const val ID = "BUNGEECORD_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = listOf(BungeeCordConstants.HANDLER_ANNOTATION)
    val LISTENER_ANNOTATIONS = listOf(BungeeCordConstants.HANDLER_ANNOTATION)

    init {
        CommonColors.applyStandardColors(colorMap, BungeeCordConstants.CHAT_COLOR_CLASS)
    }

    override val platformType = PlatformType.BUNGEECORD
    override val icon = PlatformAssets.BUNGEECORD_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS

    override fun generateModule(facet: MinecraftFacet) = BungeeCordModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BungeeCordEventGenerationPanel(chosenClass)
}
