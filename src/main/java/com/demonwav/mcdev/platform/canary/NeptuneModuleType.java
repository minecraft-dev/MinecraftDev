package com.demonwav.mcdev.platform.canary;

import com.demonwav.mcdev.asset.PlatformAssets;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class NeptuneModuleType extends CanaryModuleType {

    private static final NeptuneModuleType instance = new NeptuneModuleType();

    private static final String ID = "NEPTUNE_MODULE_TYPE";

    private NeptuneModuleType() {
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

    @Override
    public Icon getIcon() {
        return PlatformAssets.NEPTUNE_ICON;
    }

}
