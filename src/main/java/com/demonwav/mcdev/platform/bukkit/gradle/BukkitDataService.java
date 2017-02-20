/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class BukkitDataService extends AbstractDataService {

    public BukkitDataService() {
        super(BukkitModuleType.getInstance());
    }

    public BukkitDataService(@NotNull final AbstractModuleType abstractModuleType) {
        super(abstractModuleType);
    }
}
