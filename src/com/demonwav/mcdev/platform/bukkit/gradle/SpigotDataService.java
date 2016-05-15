package com.demonwav.mcdev.platform.bukkit.gradle;

import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;

import org.jetbrains.annotations.NotNull;

public class SpigotDataService extends BukkitDataService {
    public SpigotDataService() {
        super(SpigotModuleType.getInstance());
    }

    public SpigotDataService(@NotNull final MinecraftModuleType minecraftModuleType) {
        super(minecraftModuleType);
    }
}
