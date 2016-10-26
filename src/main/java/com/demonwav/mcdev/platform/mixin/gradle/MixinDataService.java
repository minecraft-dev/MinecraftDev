/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;

public class MixinDataService extends AbstractDataService {

    public MixinDataService() {
        super(MixinModuleType.getInstance());
    }
}
