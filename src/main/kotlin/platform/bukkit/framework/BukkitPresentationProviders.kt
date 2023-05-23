/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

class OldPaperPresentationProvider :
    MavenLibraryPresentationProvider(PAPER_LIBRARY_KIND, "com.destroystokyo.paper", "paper-api") {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.PAPER_ICON
}

class PaperPresentationProvider :
    MavenLibraryPresentationProvider(PAPER_LIBRARY_KIND, "io.papermc.paper", "paper-api", false) {
    override fun getIcon(properties: LibraryVersionProperties?) = PlatformAssets.PAPER_ICON
}
