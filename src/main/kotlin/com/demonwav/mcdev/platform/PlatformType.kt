/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
import com.demonwav.mcdev.platform.bungeecord.framework.BUNGEECORD_LIBRARY_KIND
import com.demonwav.mcdev.platform.canary.CanaryModuleType
import com.demonwav.mcdev.platform.canary.NeptuneModuleType
import com.demonwav.mcdev.platform.canary.framework.CANARY_LIBRARY_KIND
import com.demonwav.mcdev.platform.canary.framework.NEPTUNE_LIBRARY_KIND
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
    val children: Array<PlatformType> = arrayOf()
) {

    PAPER(PaperModuleType, "Paper"),
    SPIGOT(SpigotModuleType, "Spigot", arrayOf(PAPER)),
    BUKKIT(BukkitModuleType, "Bukkit", arrayOf(SPIGOT, PAPER)),
    FORGE(ForgeModuleType, "Forge"),
    SPONGE(SpongeModuleType, "Sponge"),
    NEPTUNE(NeptuneModuleType, "Neptune"),
    CANARY(CanaryModuleType, "Canary", arrayOf(NEPTUNE)),
    BUNGEECORD(BungeeCordModuleType, "BungeeCord"),
    LITELOADER(LiteLoaderModuleType, "LiteLoader"),
    MIXIN(MixinModuleType, "Mixin"),
    MCP(McpModuleType, "MCP");

    companion object {

        /**
         * This is legacy code which used arrays, and needed to switch to Sets...
         * Would certainly be better to just use Sets for everything, rather than this array business
         * but I'm too lazy right now. This is not hot code.
         */
        fun removeParents(types: MutableSet<PlatformType>) {
            val typesArray = types.toTypedArray()
            val result = arrayOfNulls<PlatformType>(types.size)

            var count = 0
            for (i in typesArray.indices) {
                // This has no children, so add it by default and continue to the next
                if (typesArray[i].children.isEmpty()) {
                    result[count++] = typesArray[i]
                    continue
                }

                // This has children, so check if it's children are also in the array
                var foundChild = false

                for (j in typesArray.indices) {
                    for (k in typesArray[i].children.indices) {
                        if (typesArray[j] == typesArray[i].children[k]) {
                            // It has a child in the array, stop checking
                            foundChild = true
                            break
                        }
                    }
                    // We found a child, so don't bother checking any more
                    if (foundChild) {
                        break
                    }
                }

                // We found a child, we won't add this type to the result
                if (foundChild) {
                    continue
                }

                // This type has children, but none of them are in the array, so add it to the result
                result[count++] = typesArray[i]
            }

            types.clear()
            for (i in 0 until count) {
                types.add(result[i]!!)
            }
        }

        fun fromLibraryKind(kind: LibraryKind): PlatformType? {
            return when (kind) {
                BUKKIT_LIBRARY_KIND -> BUKKIT
                SPIGOT_LIBRARY_KIND -> SPIGOT
                PAPER_LIBRARY_KIND -> PAPER
                SPONGE_LIBRARY_KIND -> SPONGE
                FORGE_LIBRARY_KIND -> FORGE
                LITELOADER_LIBRARY_KIND -> LITELOADER
                MCP_LIBRARY_KIND -> MCP
                MIXIN_LIBRARY_KIND -> MIXIN
                BUNGEECORD_LIBRARY_KIND -> BUNGEECORD
                CANARY_LIBRARY_KIND -> CANARY
                NEPTUNE_LIBRARY_KIND -> NEPTUNE
                else -> null
            }
        }
    }
}
