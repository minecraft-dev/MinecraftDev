package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class ForgeModuleType extends AbstractModuleType<ForgeModule> {

    private static final String ID = "FORGE_MODULE_TYPE";
    private static final ForgeModuleType instance = new ForgeModuleType();

    private final HashMap<String, Color> colorMap = new HashMap<>();

    private ForgeModuleType() {
        super("net.minecraftforge.gradle", "ForgeGradle");
        setupMap();
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

    @NotNull
    @Override
    public ForgeModule generateModule(Module module) {
        return new ForgeModule(module);
    }

    private void setupMap() {
        colorMap.put("net.minecraft.util.text.TextFormatting.AQUA", new Color(0x55FFFF));
        colorMap.put("net.minecraft.util.text.TextFormatting.BLACK", new Color(0x000000));
        colorMap.put("net.minecraft.util.text.TextFormatting.BLUE", new Color(0x5555FF));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_AQUA", new Color(0x00AAAA));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_BLUE", new Color(0x0000AA));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_GRAY", new Color(0x555555));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_GREEN", new Color(0x00AA00));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_PURPLE", new Color(0xAA00AA));
        colorMap.put("net.minecraft.util.text.TextFormatting.DARK_RED", new Color(0xAA0000));
        colorMap.put("net.minecraft.util.text.TextFormatting.GOLD", new Color(0xFFAA00));
        colorMap.put("net.minecraft.util.text.TextFormatting.GRAY", new Color(0xAAAAAA));
        colorMap.put("net.minecraft.util.text.TextFormatting.GREEN", new Color(0x55FF55));
        colorMap.put("net.minecraft.util.text.TextFormatting.LIGHT_PURPLE", new Color(0xFF55FF));
        colorMap.put("net.minecraft.util.text.TextFormatting.RED", new Color(0xFF5555));
        colorMap.put("net.minecraft.util.text.TextFormatting.WHITE", new Color(0xFFFFFF));
        colorMap.put("net.minecraft.util.text.TextFormatting.YELLOW", new Color(0xFFFF55));
    }

    @NotNull
    @Override
    public HashMap<String, Color> getClassToColorMappings() {
        return colorMap;
    }
}
