package com.demonwav.mcdev.platform.canary.gradle;

import com.demonwav.mcdev.platform.canary.NeptuneModuleType;

public class NeptuneDataService extends CanaryDataService {

    public NeptuneDataService() {
        super(NeptuneModuleType.getInstance());
    }

}
