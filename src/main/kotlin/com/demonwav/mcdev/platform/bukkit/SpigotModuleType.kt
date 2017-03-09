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
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object SpigotModuleType : AbstractModuleType<BukkitModule<SpigotModuleType>>("org.spigotmc", "spigot-api") {

    private const val ID = "SPIGOT_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, BukkitConstants.CHAT_COLOR_CLASS)
        CommonColors.applyStandardColors(colorMap, BungeeCordConstants.CHAT_COLOR_CLASS)
    }

    override fun getPlatformType() = PlatformType.SPIGOT
    override fun getIcon() = PlatformAssets.SPIGOT_ICON
    override fun getId() = ID
    override fun getIgnoredAnnotations() = BukkitModuleType.IGNORED_ANNOTATIONS
    override fun getListenerAnnotations() = BukkitModuleType.LISTENER_ANNOTATIONS
    override fun generateModule(facet: MinecraftFacet): BukkitModule<SpigotModuleType> = BukkitModule(facet, this)
    override fun isEventGenAvailable() = true
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BukkitEventGenerationPanel(chosenClass)
}
