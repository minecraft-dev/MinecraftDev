package com.demonwav.mcdev.platform.mixin.gradle;

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;

public class MixinDataService extends AbstractDataService {

    public MixinDataService() {
        super(MixinModuleType.getInstance());
    }
}
