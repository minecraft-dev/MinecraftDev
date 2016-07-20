package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class SpongeModuleType extends AbstractModuleType<SpongeModule> {

    private static final String ID = "SPONGE_MODULE_TYPE";
    private static final SpongeModuleType instance = new SpongeModuleType();

    private final HashMap<String, Color> colorMap = new HashMap<>();

    private SpongeModuleType() {
        super("org.spongepowered", "spongeapi");

        colorMap.put("org.spongepowered.api.text.format.TextColors.AQUA", new Color(0x55FFFF));
        colorMap.put("org.spongepowered.api.text.format.TextColors.BLACK", new Color(0x000000));
        colorMap.put("org.spongepowered.api.text.format.TextColors.BLUE", new Color(0x5555FF));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_AQUA", new Color(0x00AAAA));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_BLUE", new Color(0x0000AA));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_GRAY", new Color(0x555555));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_GREEN", new Color(0x00AA00));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_PURPLE", new Color(0xAA00AA));
        colorMap.put("org.spongepowered.api.text.format.TextColors.DARK_RED", new Color(0xAA0000));
        colorMap.put("org.spongepowered.api.text.format.TextColors.GOLD", new Color(0xFFAA00));
        colorMap.put("org.spongepowered.api.text.format.TextColors.GRAY", new Color(0xAAAAAA));
        colorMap.put("org.spongepowered.api.text.format.TextColors.GREEN", new Color(0x55FF55));
        colorMap.put("org.spongepowered.api.text.format.TextColors.LIGHT_PURPLE", new Color(0xFF55FF));
        colorMap.put("org.spongepowered.api.text.format.TextColors.RED", new Color(0xFF5555));
        colorMap.put("org.spongepowered.api.text.format.TextColors.WHITE", new Color(0xFFFFFF));
        colorMap.put("org.spongepowered.api.text.format.TextColors.YELLOW", new Color(0xFFFF55));
    }

    public static SpongeModuleType getInstance() {
        return instance;
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
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener", "org.spongepowered.api.plugin.Plugin");
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of("org.spongepowered.api.event.Listener");
    }

    @NotNull
    @Override
    public Map<String, Color> getClassToColorMappings() {
        return colorMap;
    }

    @NotNull
    @Override
    public SpongeModule generateModule(Module module) {
        return new SpongeModule(module);
    }
}
