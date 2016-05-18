package com.demonwav.mcdev.platform.forge.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;

public class ForgeDataService extends AbstractDataService {
    public ForgeDataService() {
        super(ForgeModuleType.getInstance());
    }
}
