/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.platform.adventure.AdventureModuleType
import com.demonwav.mcdev.platform.adventure.framework.ADVENTURE_LIBRARY_KIND
import com.demonwav.mcdev.platform.architectury.ArchitecturyModuleType
import com.demonwav.mcdev.platform.architectury.framework.ARCHITECTURY_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType
import com.demonwav.mcdev.platform.bukkit.PaperModuleType
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType
import com.demonwav.mcdev.platform.bukkit.framework.BUKKIT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.PAPER_LIBRARY_KIND
import com.demonwav.mcdev.platform.bukkit.framework.SPIGOT_LIBRARY_KIND
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType
import com.demonwav.mcdev.platform.bungeecord.WaterfallModuleType
import com.demonwav.mcdev.platform.bungeecord.framework.BUNGEECORD_LIBRARY_KIND
import com.demonwav.mcdev.platform.bungeecord.framework.WATERFALL_LIBRARY_KIND
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.fabric.framework.FABRIC_LIBRARY_KIND
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.framework.FORGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.framework.MCP_LIBRARY_KIND
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.framework.MIXIN_LIBRARY_KIND
import com.demonwav.mcdev.platform.neoforge.NeoForgeModuleType
import com.demonwav.mcdev.platform.neoforge.framework.NEOFORGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.velocity.VelocityModuleType
import com.demonwav.mcdev.platform.velocity.framework.VELOCITY_LIBRARY_KIND
import com.intellij.openapi.roots.libraries.LibraryKind

enum class PlatformType(
    val type: AbstractModuleType<*>,
    val versionJson: String? = null,
    private val parent: PlatformType? = null,
) {

    BUKKIT(BukkitModuleType, "bukkit.json"),
    SPIGOT(SpigotModuleType, "spigot.json", BUKKIT),
    PAPER(PaperModuleType, "paper.json", SPIGOT),
    ARCHITECTURY(ArchitecturyModuleType),
    FORGE(ForgeModuleType),
    FABRIC(FabricModuleType),
    SPONGE(SpongeModuleType),
    BUNGEECORD(BungeeCordModuleType, "bungeecord_v2.json"),
    WATERFALL(WaterfallModuleType, "waterfall.json", BUNGEECORD),
    VELOCITY(VelocityModuleType, "velocity.json"),
    MIXIN(MixinModuleType),
    NEOFORGE(NeoForgeModuleType),
    MCP(McpModuleType),
    ADVENTURE(AdventureModuleType),
    ;

    private val children = mutableListOf<PlatformType>()

    init {
        parent?.addChild(this)
    }

    private fun addChild(child: PlatformType) {
        children += child
        parent?.addChild(child)
    }

    companion object {
        fun removeParents(types: Set<PlatformType?>) =
            types.asSequence()
                .filterNotNull()
                .filter { type -> type.children.isEmpty() || types.none { type.children.contains(it) } }
                .toHashSet()

        fun fromLibraryKind(kind: LibraryKind) = when (kind) {
            BUKKIT_LIBRARY_KIND -> BUKKIT
            SPIGOT_LIBRARY_KIND -> SPIGOT
            PAPER_LIBRARY_KIND -> PAPER
            SPONGE_LIBRARY_KIND -> SPONGE
            ARCHITECTURY_LIBRARY_KIND -> ARCHITECTURY
            FORGE_LIBRARY_KIND -> FORGE
            FABRIC_LIBRARY_KIND -> FABRIC
            MCP_LIBRARY_KIND -> MCP
            MIXIN_LIBRARY_KIND -> MIXIN
            BUNGEECORD_LIBRARY_KIND -> BUNGEECORD
            WATERFALL_LIBRARY_KIND -> WATERFALL
            VELOCITY_LIBRARY_KIND -> VELOCITY
            ADVENTURE_LIBRARY_KIND -> ADVENTURE
            NEOFORGE_LIBRARY_KIND -> NEOFORGE
            else -> null
        }
    }
}
