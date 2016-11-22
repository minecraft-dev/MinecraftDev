/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;

public class BungeeCordDataService extends AbstractDataService {

    public BungeeCordDataService() {
        super(BungeeCordModuleType.getInstance());
    }
}
