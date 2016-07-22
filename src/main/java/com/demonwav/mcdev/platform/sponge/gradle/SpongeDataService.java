package com.demonwav.mcdev.platform.sponge.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

public class SpongeDataService extends AbstractDataService {

    public SpongeDataService() {
        super(SpongeModuleType.getInstance());
    }
}
