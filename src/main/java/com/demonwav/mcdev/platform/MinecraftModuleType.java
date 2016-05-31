package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.creator.MinecraftModuleBuilder;

import com.google.common.base.Strings;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.Iterator;

public class MinecraftModuleType extends JavaModuleType {

    @NotNull
    private static final String ID = "MINECRAFT_MODULE_TYPE";
    public static final String OPTION = "com.demonwav.mcdev.MinecraftModuleTypes";

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
        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            minecraftModule.addModuleType(option);
        }
    }

    public static void removeOption(@NotNull Module module, @NotNull String option) {
        String currentOption = module.getOptionValue(OPTION);
        if (Strings.isNullOrEmpty(currentOption)) {
            return;
        }

        if (currentOption.contains(option)) {
            String[] parts = currentOption.split(",");
            String newOption = "";
            Iterator<String> partIterator = Arrays.asList(parts).iterator();
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

        MinecraftModule minecraftModule = MinecraftModule.getInstance(module);
        if (minecraftModule != null) {
            minecraftModule.removeModuleType(option);
        }
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
