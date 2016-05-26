package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

public class SpongeModuleType extends AbstractModuleType {

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
        if (UIUtil.isUnderDarcula()) {
            return PlatformAssets.SPONGE_ICON_2X;
        } else {
            return PlatformAssets.SPONGE_ICON_DARK_2X;
        }
    }

    @Override
    public Icon getIcon() {
        if (UIUtil.isUnderDarcula()) {
            return PlatformAssets.SPONGE_ICON;
        } else {
            return PlatformAssets.SPONGE_ICON_DARK;
        }
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        if (UIUtil.isUnderDarcula()) {
            return PlatformAssets.SPONGE_ICON_2X;
        } else {
            return PlatformAssets.SPONGE_ICON_DARK_2X;
        }
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener", "org.spongepowered.api.plugin.Plugin");
    }

    @Override
    public AbstractModule generateModule(Module module) {
        return new SpongeModule(module);
    }

    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener");
    }
}
