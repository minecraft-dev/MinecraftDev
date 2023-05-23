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

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.platform.adventure.framework.ADVENTURE_LIBRARY_KIND
import com.demonwav.mcdev.platform.architectury.framework.ARCHITECTURY_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.BUKKIT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.PAPER_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.SPIGOT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bungeecord.framework.BUNGEECORD_LIBRARY_KIND
import com.demonwav.mcdev.platform.bungeecord.framework.WATERFALL_LIBRARY_KIND
import com.demonwav.mcdev.platform.fabric.framework.FABRIC_LIBRARY_KIND
import com.demonwav.mcdev.platform.forge.framework.FORGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.mcp.framework.MCP_LIBRARY_KIND
import com.demonwav.mcdev.platform.mixin.framework.MIXIN_LIBRARY_KIND
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.velocity.framework.VELOCITY_LIBRARY_KIND

val MINECRAFT_LIBRARY_KINDS = setOf(
    BUKKIT_LIBRARY_KIND,
    SPIGOT_LIBRARY_KIND,
    PAPER_LIBRARY_KIND,
    SPONGE_LIBRARY_KIND,
    FORGE_LIBRARY_KIND,
    FABRIC_LIBRARY_KIND,
    ARCHITECTURY_LIBRARY_KIND,
    MCP_LIBRARY_KIND,
    MIXIN_LIBRARY_KIND,
    BUNGEECORD_LIBRARY_KIND,
    WATERFALL_LIBRARY_KIND,
    VELOCITY_LIBRARY_KIND,
    ADVENTURE_LIBRARY_KIND,
)
