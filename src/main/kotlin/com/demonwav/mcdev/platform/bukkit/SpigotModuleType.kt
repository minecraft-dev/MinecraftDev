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
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.psi.PsiClass

object SpigotModuleType : AbstractModuleType<BukkitModule<SpigotModuleType>>("org.spigotmc", "spigot-api") {

    private const val ID = "SPIGOT_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, BukkitConstants.CHAT_COLOR_CLASS)
        CommonColors.applyStandardColors(colorMap, BungeeCordConstants.CHAT_COLOR_CLASS)
    }

    override val platformType = PlatformType.SPIGOT
    override val icon = PlatformAssets.SPIGOT_ICON
    override val id = ID
    override val ignoredAnnotations = BukkitModuleType.IGNORED_ANNOTATIONS
    override val listenerAnnotations = BukkitModuleType.LISTENER_ANNOTATIONS
    override val isEventGenAvailable = true

    override fun generateModule(facet: MinecraftFacet): BukkitModule<SpigotModuleType> = BukkitModule(facet, this)
    override fun getEventGenerationPanel(chosenClass: PsiClass) = BukkitEventGenerationPanel(chosenClass)
}
