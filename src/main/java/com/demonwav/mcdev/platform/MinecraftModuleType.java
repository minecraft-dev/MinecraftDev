/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.creator.MinecraftModuleBuilder;
import com.google.common.base.Strings;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public class MinecraftModuleType extends JavaModuleType {

    @NotNull
    private static final String ID = "MINECRAFT_MODULE_TYPE";
    public static final String OPTION = "com.demonwav.mcdev.MinecraftModuleTypes";

    @NotNull
    public static MinecraftModuleType getInstance() {
        return (MinecraftModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    public static void addOption(@NotNull Module module, @NotNull String option) {
        String currentOption = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(currentOption)) {
            currentOption = option;
        } else {
            if (!currentOption.contains(option)) {
                currentOption += "," + option;
            }
        }
        module.setOption(OPTION, currentOption);
        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            final PlatformType[] types = cleanOption(module);
            minecraftModule.updateModules(types);
        }
    }

    public static void removeOption(@NotNull Module module, @NotNull String option) {
        final String currentOption = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(currentOption)) {
            return;
        }

        if (currentOption.contains(option)) {
            final String[] parts = currentOption.split(",");
            String newOption = "";
            final Iterator<String> partIterator = Arrays.asList(parts).iterator();
            while (partIterator.hasNext()) {
                String part = partIterator.next();

                if (part.equals(option)) {
                    continue;
                }

                newOption += part;

                if (partIterator.hasNext()) {
                    newOption += ",";
                }
            }

            module.setOption(OPTION, newOption);
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            final PlatformType[] types = cleanOption(module);
            minecraftModule.updateModules(types);
        }
    }

    private static PlatformType[] cleanOption(@NotNull Module module) {
        String option = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(option)) {
            return new PlatformType[0];
        }

        // Remove ,'s at the beginning of the text
        option = option.replaceAll("^,+", "");
        // Remove ,'s at the end of the text
        option = option.replaceAll(",+$", "");
        // Remove duplicate ,'s
        option = option.replaceAll(",{2,}", ",");

        // Remove parent types
        final String[] split = option.split(",");
        PlatformType[] types = new PlatformType[split.length];
        for (int i = 0; i < split.length; i++) {
            types[i] = PlatformType.getTypeByName(split[i]);
        }

        final PlatformType[] finalTypes = PlatformType.removeParents(types);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < finalTypes.length; i++) {
            sb.append(finalTypes[i].getName());
            if (i != finalTypes.length - 1) {
                sb.append(",");
            }
        }

        module.setOption(OPTION, sb.toString());

        return finalTypes;
    }

    @NotNull
    @Override
    public MinecraftModuleBuilder createModuleBuilder() {
        return new MinecraftModuleBuilder();
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.MINECRAFT_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.MINECRAFT_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.MINECRAFT_ICON;
    }
}
