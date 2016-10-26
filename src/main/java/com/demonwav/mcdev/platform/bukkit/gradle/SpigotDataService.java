/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.gradle;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.bukkit.SpigotModuleType;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SpigotDataService extends BukkitDataService {

    public SpigotDataService() {
        super(SpigotModuleType.getInstance());
    }

    public SpigotDataService(@NotNull final AbstractModuleType abstractModuleType) {
        super(abstractModuleType);
    }
}
