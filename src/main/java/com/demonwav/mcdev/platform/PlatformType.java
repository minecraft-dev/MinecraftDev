package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PlatformType {

    BUKKIT(BukkitModuleType.getInstance(), "BUKKIT_MODULE_TYPE", "Bukkit"),
    SPIGOT(SpigotModuleType.getInstance(), "SPIGOT_MODULE_TYPE", "Spigot"),
    PAPER(PaperModuleType.getInstance(), "PAPER_MODULE_TYPE", "Paper"),
    FORGE(ForgeModuleType.getInstance(), "FORGE_MODULE_TYPE", "Forge"),
    SPONGE(SpongeModuleType.getInstance(), "SPONGE_MODULE_TYPE", "Sponge"),
    BUNGEECORD(BungeeCordModuleType.getInstance(), "BUNGEECORD_MODULE_TYPE", "BungeeCord"),
    LITELOADER(LiteLoaderModuleType.getInstance(), "LITELOADER_MODULE_TYPE", "LiteLoader"),
    MIXIN(MixinModuleType.getInstance(), "MIXIN_MODULE_TYPE", "Mixin"),
    ;

    private final AbstractModuleType<?> type;
    private final String name;
    private final String normalName;

    PlatformType(final AbstractModuleType<?> type, final String name, final String normalName) {
        this.type = type;
        this.name = name;
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
