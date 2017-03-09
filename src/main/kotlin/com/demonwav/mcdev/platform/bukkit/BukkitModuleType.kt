/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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

    override fun getPlatformType() = PlatformType.BUKKIT
    override fun getIcon() = PlatformAssets.BUKKIT_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = LISTENER_ANNOTATIONS
    override fun generateModule(facet: MinecraftFacet): BukkitModule<BukkitModuleType> = BukkitModule(facet, this)
    override fun isEventGenAvailable() = true
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BukkitEventGenerationPanel(chosenClass)
}
