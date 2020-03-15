/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.framework

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MavenLibraryPresentationProvider
import com.intellij.framework.library.LibraryVersionProperties

class BukkitPresentationProvider : MavenLibraryPresentationProvider(BUKKIT_LIBRARY_KIND, "org.bukkit", "bukkit") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.BUKKIT_ICON
}

class SpigotPresentationProvider : MavenLibraryPresentationProvider(SPIGOT_LIBRARY_KIND, "org.spigotmc", "spigot-api") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.SPIGOT_ICON
}

class PaperPresentationProvider :
    MavenLibraryPresentationProvider(PAPER_LIBRARY_KIND, "com.destroystokyo.paper", "paper-api") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.PAPER_ICON
}
