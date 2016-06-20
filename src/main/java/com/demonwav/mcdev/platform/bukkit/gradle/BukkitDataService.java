package com.demonwav.mcdev.platform.bukkit.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;

import org.jetbrains.annotations.NotNull;

public class BukkitDataService extends AbstractDataService {
    public BukkitDataService() {
        super(BukkitModuleType.getInstance());
    }

    public BukkitDataService(@NotNull final AbstractModuleType abstractModuleType) {
        super(abstractModuleType);
    }
}
