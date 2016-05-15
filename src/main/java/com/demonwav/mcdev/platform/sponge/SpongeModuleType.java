package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class SpongeModuleType extends MinecraftModuleType {

    private static final String ID = "SPONGE_MODULE_TYPE";

    public SpongeModuleType() {
        super(ID, "org.spongepowered", "spongeapi");
    }

    public static SpongeModuleType getInstance() {
        return (SpongeModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.SPONGE;
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.SPONGE_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.SPONGE_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.SPONGE_ICON;
    }
}
