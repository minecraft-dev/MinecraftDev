package com.demonwav.mcdev.platform.canary;

import org.jetbrains.annotations.NotNull;

public class NeptuneModuleType extends CanaryModuleType {

    private static final NeptuneModuleType instance = new NeptuneModuleType();

    private static final String ID = "NEPTUNE_MODULE_TYPE";

    protected NeptuneModuleType() {
        super("org.neptunepowered", "NeptuneLib");
    }

    @NotNull
    public static NeptuneModuleType getInstance() {
        return instance;
    }

    @Override
    public String getId() {
        return ID;
    }

}
