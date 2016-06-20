package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PlatformType {
    BUKKIT(BukkitModuleType.getInstance(), "BUKKIT_MODULE_TYPE"),
    SPIGOT(SpigotModuleType.getInstance(), "SPIGOT_MODULE_TYPE"),
    PAPER(PaperModuleType.getInstance(), "PAPER_MODULE_TYPE"),
    FORGE(ForgeModuleType.getInstance(), "FORGE_MODULE_TYPE"),
    SPONGE(SpongeModuleType.getInstance(), "SPONGE_MODULE_TYPE"),
    BUNGEECORD(BungeeCordModuleType.getInstance(), "BUNGEECORD_MODULE_TYPE");

    private final AbstractModuleType<?> type;
    private final String name;

    PlatformType(final AbstractModuleType<?> type, final String name) {
        this.type = type;
        this.name = name;
    }

    @NotNull
    public AbstractModuleType getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return name;
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
