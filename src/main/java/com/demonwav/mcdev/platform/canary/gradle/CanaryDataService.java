/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.canary.CanaryModuleType;
import org.jetbrains.annotations.NotNull;

public class CanaryDataService extends AbstractDataService {

    public CanaryDataService() {
        this(CanaryModuleType.INSTANCE);
    }

    public CanaryDataService(@NotNull AbstractModuleType type) {
        super(type);
    }

}
