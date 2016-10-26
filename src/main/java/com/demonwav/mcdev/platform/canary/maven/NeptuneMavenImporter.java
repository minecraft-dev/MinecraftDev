/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.maven;

import com.demonwav.mcdev.platform.canary.NeptuneModuleType;

public class NeptuneMavenImporter extends CanaryMavenImporter {

    public NeptuneMavenImporter() {
        super(NeptuneModuleType.getInstance());
    }

}
