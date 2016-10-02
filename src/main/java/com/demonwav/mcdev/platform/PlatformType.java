package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PlatformType {

    BUKKIT(BukkitModuleType.getInstance(), "Bukkit"),
    SPIGOT(SpigotModuleType.getInstance(), "Spigot"),
    PAPER(PaperModuleType.getInstance(), "Paper"),
    FORGE(ForgeModuleType.getInstance(), "Forge"),
    SPONGE(SpongeModuleType.getInstance(), "Sponge"),
    BUNGEECORD(BungeeCordModuleType.getInstance(), "BungeeCord"),
    LITELOADER(LiteLoaderModuleType.getInstance(), "LiteLoader"),
    MIXIN(MixinModuleType.getInstance(), "Mixin"),
    MCP(McpModuleType.getInstance(), "MCP")
    ;

    private final AbstractModuleType<?> type;
    private final String name;
    private final String normalName;

    PlatformType(final AbstractModuleType<?> type, final String normalName) {
        this.type = type;
        this.name = type.getId();
        this.normalName = normalName;
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
    public static AbstractModuleType<?> getByName(String name) {
        for (PlatformType type : values()) {
            if (type.getName().equals(name)) {
                return type.getType();
            }
        }
        return null;
    }
}
