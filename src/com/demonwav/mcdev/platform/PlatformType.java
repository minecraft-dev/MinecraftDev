package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

public enum PlatformType {
    BUKKIT(BukkitModuleType.getInstance()),
    SPIGOT(SpigotModuleType.getInstance()),
    PAPER(PaperModuleType.getInstance()),
    FORGE(null),
    SPONGE(SpongeModuleType.getInstance()),
    BUNGEECORD(BungeeCordModuleType.getInstance());

    private final MinecraftModuleType type;

    PlatformType(final MinecraftModuleType type) {
        this.type = type;
    }

    public MinecraftModuleType getType() {
        return type;
    }
}
