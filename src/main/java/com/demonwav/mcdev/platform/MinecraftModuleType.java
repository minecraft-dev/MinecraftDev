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
import com.google.common.collect.Sets;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
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
        addOption(module, option, true);
    }

    public static void addOption(@NotNull Module module, @NotNull String option, boolean updateModule) {
        String currentOption = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(currentOption)) {
            currentOption = option;
        } else {
            if (!currentOption.contains(option)) {
                currentOption += "," + option;
            }
        }

        final String finalOption = cleanOption(currentOption);
        module.setOption(OPTION, finalOption);

        if (!updateModule) {
            return;
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            final PlatformType[] types = getTypes(finalOption);
            minecraftModule.updateModules(types);
        }
    }

    public static void removeOption(@NotNull Module module, @NotNull String option) {
        removeOption(module, option, true);
    }

    public static void removeOption(@NotNull Module module, @NotNull String option, boolean updateModule) {
        final String currentOption = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(currentOption)) {
            return;
        }

        if (!currentOption.contains(option)) {
            return;
        }

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

        final String finalOption = cleanOption(newOption);
        module.setOption(OPTION, finalOption);


        if (!updateModule) {
            return;
        }

        final MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            final PlatformType[] types = getTypes(finalOption);
            minecraftModule.updateModules(types);
        }
    }

    @NotNull
    private static String cleanOption(@NotNull String option) {
        if (Strings.isNullOrEmpty(option)) {
            return "";
        }

        // Remove ,'s at the beginning of the text
        option = option.replaceAll("^,+", "");
        // Remove ,'s at the end of the text
        option = option.replaceAll(",+$", "");
        // Remove duplicate ,'s
        option = option.replaceAll(",{2,}", ",");

        // Remove parent types
        final Set<PlatformType> typesSet = PlatformType.removeParents(Sets.newHashSet(getTypes(option)));
        final PlatformType[] types = typesSet.toArray(new PlatformType[typesSet.size()]);

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            sb.append(types[i].getName());
            if (i != types.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    private static PlatformType[] getTypes(@NotNull String option) {
        final String[] split = option.split(",");
        if (split.length == 1 && split[0].isEmpty()) {
            return new PlatformType[0];
        }

        final PlatformType[] types = new PlatformType[split.length];
        for (int i = 0; i < split.length; i++) {
            types[i] = PlatformType.getTypeByName(split[i]);
        }

        return types;
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
