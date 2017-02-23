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
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.generation.BukkitEventGenerationPanel
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass

object PaperModuleType : AbstractModuleType<BukkitModule<PaperModuleType>>("com.destroystokyo.paper", "paper-api") {

    private const val ID = "PAPER_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, BukkitConstants.CHAT_COLOR_CLASS)
        CommonColors.applyStandardColors(colorMap, BungeeCordConstants.CHAT_COLOR_CLASS)
    }

    override fun getPlatformType() = PlatformType.PAPER
    override fun getIcon() = PlatformAssets.PAPER_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = BukkitModuleType.IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = BukkitModuleType.LISTENER_ANNOTATIONS
    override fun generateModule(module: Module): BukkitModule<PaperModuleType> = BukkitModule(module, this)
    override fun isEventGenAvailable() = true
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BukkitEventGenerationPanel(chosenClass)
}
