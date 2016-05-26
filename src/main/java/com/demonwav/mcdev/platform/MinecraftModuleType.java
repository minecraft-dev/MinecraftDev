package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

public class MinecraftModuleType extends JavaModuleType {

    @NotNull
    private static final String ID = "MINECRAFT_MODULE_TYPE";
    public static final String OPTION = "com.demonwav.mcdev.MinecraftModuleTypes";

    public static MinecraftModuleType getInstance() {
        return (MinecraftModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public Icon getBigIcon() {
        if (moduleTypes.size() == 0 || moduleTypes.size() != 1) {
            return PlatformAssets.MINECRAFT_ICON_2X;
        } else {
            return moduleTypes.get(0).getBigIcon();
        }
    }

    @Override
    public Icon getIcon() {
        if (moduleTypes.size() == 0 || moduleTypes.size() != 1) {
            return PlatformAssets.MINECRAFT_ICON;
        } else {
            return moduleTypes.get(0).getIcon();
        }
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        if (moduleTypes.size() == 0 || moduleTypes.size() != 1) {
            return PlatformAssets.MINECRAFT_ICON;
        } else {
            return moduleTypes.get(0).getNodeIcon(isOpened);
        }
    }

    public List<String> getIgnoredAnnotations() {
        List<String> ignoredAnnotations = new ArrayList<>();
        moduleTypes.stream().forEach(m -> ignoredAnnotations.addAll(m.getIgnoredAnnotations()));
        return ignoredAnnotations;
    }

    public MinecraftModule generateModule(Module module) {
        return MinecraftModule.generate(moduleTypes, module);
    }

    public List<String> getListenerAnnotations() {
        List<String> listenerAnnotations = new ArrayList<>();
        moduleTypes.stream().forEach(m -> listenerAnnotations.addAll(m.getListenerAnnotations()));
        return listenerAnnotations;
    }
}
