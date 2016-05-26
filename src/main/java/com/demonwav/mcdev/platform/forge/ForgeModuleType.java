package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

public class ForgeModuleType extends AbstractModuleType {

    private static final String ID = "FORGE_MODULE_TYPE";

    public ForgeModuleType() {
        super(ID, "net.minecraftforge.gradle", "ForgeGradle");
    }

    public static ForgeModuleType getInstance() {
        return (ForgeModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.FORGE_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.FORGE_ICON;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of(
                "net.minecraftforge.fml.common.Mod",
                "net.minecraftforge.fml.common.Mod.EventHandler",
                "net.minecraftforge.fml.common.eventhandler.SubscribeEvent"
        );
    }

    @Override
    public AbstractModule generateModule(Module module) {
        return new ForgeModule(module);
    }

    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of(
                "net.minecraftforge.fml.common.Mod.EventHandler",
                "net.minecraftforge.fml.common.eventhandler.SubscribeEvent"
        );
    }
}
