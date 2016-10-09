/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.bukkit.gradle;

import com.demonwav.mcdev.platform.bukkit.PaperModuleType;

public class PaperDataService extends SpigotDataService {

    public PaperDataService() {
        super(PaperModuleType.getInstance());
    }
}
