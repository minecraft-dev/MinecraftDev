package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

public class ForgeModuleType extends AbstractModuleType {

    private static final String ID = "FORGE_MODULE_TYPE";
    private static final ForgeModuleType instance = new ForgeModuleType();

    private ForgeModuleType() {
        super("net.minecraftforge.gradle", "ForgeGradle");
    }

    public static ForgeModuleType getInstance() {
        return instance;
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
    public String getId() {
        return ID;
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

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of(
                "net.minecraftforge.fml.common.Mod.EventHandler",
                "net.minecraftforge.fml.common.eventhandler.SubscribeEvent"
        );
    }
}
