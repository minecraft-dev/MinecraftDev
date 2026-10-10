/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

import com.demonwav.mcdev.facet.MavenMinecraftLibraryDetector
import com.demonwav.mcdev.platform.PlatformType

class BukkitLibraryDetector :
    MavenMinecraftLibraryDetector(PlatformType.BUKKIT, "org.bukkit", "bukkit")

class SpigotLibraryDetector :
    MavenMinecraftLibraryDetector(PlatformType.SPIGOT, "org.spigotmc", "spigot-api")

class OldPaperLibraryDetector :
    MavenMinecraftLibraryDetector(PlatformType.PAPER, "com.destroystokyo.paper", "paper-api")

class PaperLibraryDetector :
    MavenMinecraftLibraryDetector(PlatformType.PAPER, "io.papermc.paper", "paper-api", false)
