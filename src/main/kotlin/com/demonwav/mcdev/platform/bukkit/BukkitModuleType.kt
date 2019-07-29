/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.generation.BukkitEventGenerationPanel
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object BukkitModuleType : AbstractModuleType<BukkitModule<BukkitModuleType>>("org.bukkit", "bukkit") {

    private const val ID = "BUKKIT_MODULE_TYPE"

    val IGNORED_ANNOTATIONS = listOf(BukkitConstants.HANDLER_ANNOTATION)
    val LISTENER_ANNOTATIONS = listOf(BukkitConstants.HANDLER_ANNOTATION)

    init {
        CommonColors.applyStandardColors(colorMap, BukkitConstants.CHAT_COLOR_CLASS)
    }

    override val platformType = PlatformType.BUKKIT
    override val icon = PlatformAssets.BUKKIT_ICON
    override val id = ID
    override val ignoredAnnotations = IGNORED_ANNOTATIONS
    override val listenerAnnotations = LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet): BukkitModule<BukkitModuleType> = BukkitModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BukkitEventGenerationPanel(chosenClass)
}
