/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PlatformType {

    PAPER(PaperModuleType.getInstance(), "Paper"),
    SPIGOT(SpigotModuleType.getInstance(), "Spigot", new PlatformType[] {PAPER}),
    BUKKIT(BukkitModuleType.getInstance(), "Bukkit", new PlatformType[] {SPIGOT, PAPER}),
    FORGE(ForgeModuleType.getInstance(), "Forge"),
    SPONGE(SpongeModuleType.getInstance(), "Sponge"),
    NEPTUNE(NeptuneModuleType.getInstance(), "Neptune"),
    CANARY(CanaryModuleType.getInstance(), "Canary", new PlatformType[] {NEPTUNE}),
    BUNGEECORD(BungeeCordModuleType.getInstance(), "BungeeCord"),
    LITELOADER(LiteLoaderModuleType.getInstance(), "LiteLoader"),
    MIXIN(MixinModuleType.getInstance(), "Mixin"),
    MCP(McpModuleType.getInstance(), "MCP")
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
        final PlatformType typebyName = getTypeByName(name);
        if (typebyName == null) {
            return null;
        }
        return typebyName.getType();
    }

    @NotNull
    public static PlatformType[] removeParents(@NotNull PlatformType[] types) {
        final PlatformType[] result = new PlatformType[types.length];

        int count = 0;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < types.length; i++) {
            // This has no children, so add it by default and continue to the next
            if (types[i].children.length == 0) {
                result[count++] = types[i];
                continue;
            }

            // This has children, so check if it's children are also in the array
            boolean foundChild = false;
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0; j < types.length; j++) {
                for (int k = 0; k < types[i].children.length; k++) {
                    if (types[j] == types[i].children[k]) {
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
            result[count++] = types[i];
        }

        final PlatformType[] finalResult = new PlatformType[count];
        System.arraycopy(result, 0, finalResult, 0, count);
        return finalResult;
    }
}
