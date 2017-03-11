/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import static com.demonwav.mcdev.platform.bukkit.framework.BukkitLibraryKindKt.BUKKIT_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.bukkit.framework.BukkitLibraryKindKt.PAPER_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.bukkit.framework.BukkitLibraryKindKt.SPIGOT_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.bungeecord.framework.BungeeCordLibraryKindKt.BUNGEECORD_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.canary.framework.CanaryLibraryKindKt.CANARY_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.canary.framework.CanaryLibraryKindKt.NEPTUNE_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.forge.framework.ForgeLibraryKindKt.FORGE_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.liteloader.framework.LiteLoaderLibraryKindKt.LITELOADER_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.mcp.framework.McpLibraryKindKt.MCP_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.mixin.framework.MixinLibraryKindKt.MIXIN_LIBRARY_KIND;
import static com.demonwav.mcdev.platform.sponge.framework.SpongeLibraryKindKt.SPONGE_LIBRARY_KIND;
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.canary.CanaryModuleType;
import com.demonwav.mcdev.platform.canary.NeptuneModuleType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;
import com.google.common.collect.Sets;
import com.intellij.openapi.roots.libraries.LibraryKind;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PlatformType {

    PAPER(PaperModuleType.INSTANCE, "Paper"),
    SPIGOT(SpigotModuleType.INSTANCE, "Spigot", new PlatformType[] {PAPER}),
    BUKKIT(BukkitModuleType.INSTANCE, "Bukkit", new PlatformType[] {SPIGOT, PAPER}),
    FORGE(ForgeModuleType.INSTANCE, "Forge"),
    SPONGE(SpongeModuleType.INSTANCE, "Sponge"),
    NEPTUNE(NeptuneModuleType.INSTANCE, "Neptune"),
    CANARY(CanaryModuleType.INSTANCE, "Canary", new PlatformType[] {NEPTUNE}),
    BUNGEECORD(BungeeCordModuleType.INSTANCE, "BungeeCord"),
    LITELOADER(LiteLoaderModuleType.INSTANCE, "LiteLoader"),
    MIXIN(MixinModuleType.INSTANCE, "Mixin"),
    MCP(McpModuleType.INSTANCE, "MCP")
    ;

    private final AbstractModuleType<?> type;
    private final String name;
    private final String normalName;
    private final PlatformType[] children;

    PlatformType(final AbstractModuleType<?> type, final String normalName) {
        this.type = type;
        this.name = type.getId();
        this.normalName = normalName;
        this.children = new PlatformType[0];
    }

    PlatformType(final AbstractModuleType<?> type, final String normalName, final PlatformType[] children) {
        this.type = type;
        this.name = type.getId();
        this.normalName = normalName;
        this.children = children;
    }

    @NotNull
    @Contract(pure = true)
    public AbstractModuleType getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getNormalName() {
        return normalName;
    }

    @Nullable
    public static PlatformType getTypeByName(String name){
        for (PlatformType type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static AbstractModuleType<?> getByName(String name) {
        final PlatformType typeByName = getTypeByName(name);
        if (typeByName == null) {
            return null;
        }
        return typeByName.getType();
    }

    @NotNull
    public static Set<PlatformType> removeParents(@NotNull Set<PlatformType> types) {
        final PlatformType[] typesArray = types.toArray(new PlatformType[types.size()]);
        final PlatformType[] result = new PlatformType[types.size()];

        int count = 0;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < typesArray.length; i++) {
            // This has no children, so add it by default and continue to the next
            if (typesArray[i].children.length == 0) {
                result[count++] = typesArray[i];
                continue;
            }

            // This has children, so check if it's children are also in the array
            boolean foundChild = false;
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < typesArray.length; j++) {
                for (int k = 0; k < typesArray[i].children.length; k++) {
                    if (typesArray[j] == typesArray[i].children[k]) {
                        // It has a child in the array, stop checking
                        foundChild = true;
                        break;
                    }
                }
                // We found a child, so don't bother checking any more
                if (foundChild) {
                    break;
                }
            }

            // We found a child, we won't add this type to the result
            if (foundChild) {
                continue;
            }

            // This type has children, but none of them are in the array, so add it to the result
            result[count++] = typesArray[i];
        }

        final PlatformType[] finalResult = new PlatformType[count];
        System.arraycopy(result, 0, finalResult, 0, count);
        return Sets.newHashSet(finalResult);
    }

    @Nullable
    public static PlatformType fromLibraryKind(@NotNull LibraryKind kind) {
        if (kind == BUKKIT_LIBRARY_KIND) {
            return BUKKIT;
        } else if (kind == SPIGOT_LIBRARY_KIND) {
            return SPIGOT;
        } else if (kind == PAPER_LIBRARY_KIND) {
            return PAPER;
        } else if (kind == SPONGE_LIBRARY_KIND) {
            return SPONGE;
        } else if (kind == FORGE_LIBRARY_KIND) {
            return FORGE;
        } else if (kind == LITELOADER_LIBRARY_KIND) {
            return LITELOADER;
        } else if (kind == MCP_LIBRARY_KIND) {
            return MCP;
        } else if (kind == MIXIN_LIBRARY_KIND) {
            return MIXIN;
        } else if (kind == BUNGEECORD_LIBRARY_KIND) {
            return BUNGEECORD;
        } else if (kind == CANARY_LIBRARY_KIND) {
            return CANARY;
        } else if (kind == NEPTUNE_LIBRARY_KIND) {
            return NEPTUNE;
        }
        return null;
    }
}
