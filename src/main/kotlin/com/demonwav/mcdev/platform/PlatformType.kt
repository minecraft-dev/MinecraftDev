/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

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
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.framework.FORGE_LIBRARY_KIND
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType
import com.demonwav.mcdev.platform.liteloader.framework.LITELOADER_LIBRARY_KIND
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.framework.MCP_LIBRARY_KIND
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.framework.MIXIN_LIBRARY_KIND
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.platform.sponge.framework.SPONGE_LIBRARY_KIND
import com.intellij.openapi.roots.libraries.LibraryKind

enum class PlatformType(
    val type: AbstractModuleType<*>,
    val normalName: String,
    val versionJson: String? = null,
    val children: Array<PlatformType> = arrayOf()
) {

    PAPER(PaperModuleType, "Paper", "paper.json"),
    SPIGOT(SpigotModuleType, "Spigot", "spigot.json", arrayOf(PAPER)),
    BUKKIT(BukkitModuleType, "Bukkit", "bukkit.json", arrayOf(SPIGOT, PAPER)),
    FORGE(ForgeModuleType, "Forge"),
    SPONGE(SpongeModuleType, "Sponge"),
    WATERFALL(WaterfallModuleType, "Waterfall", "waterfall.json"),
    BUNGEECORD(BungeeCordModuleType, "BungeeCord", "bungeecord.json", arrayOf(WATERFALL)),
    LITELOADER(LiteLoaderModuleType, "LiteLoader"),
    MIXIN(MixinModuleType, "Mixin"),
    MCP(McpModuleType, "MCP");

    companion object {
        fun removeParents(types: MutableSet<PlatformType>) =
            types.filter { type -> type.children.isEmpty() || !types.any { type.children.contains(it) } }.toHashSet()

        fun fromLibraryKind(kind: LibraryKind) = when (kind) {
            BUKKIT_LIBRARY_KIND -> BUKKIT
            SPIGOT_LIBRARY_KIND -> SPIGOT
            PAPER_LIBRARY_KIND -> PAPER
            SPONGE_LIBRARY_KIND -> SPONGE
            FORGE_LIBRARY_KIND -> FORGE
            LITELOADER_LIBRARY_KIND -> LITELOADER
            MCP_LIBRARY_KIND -> MCP
            MIXIN_LIBRARY_KIND -> MIXIN
            BUNGEECORD_LIBRARY_KIND -> BUNGEECORD
            WATERFALL_LIBRARY_KIND -> WATERFALL
            else -> null
        }
    }
}
