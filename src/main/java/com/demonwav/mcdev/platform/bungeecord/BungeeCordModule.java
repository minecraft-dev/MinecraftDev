package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

public class BungeeCordModule extends AbstractModule {

    private static final Map<Module, BungeeCordModule> map = new HashMap<>();

    @NotNull
    private Module module;

    private VirtualFile pluginYml;

    private BungeeCordModule(@NotNull Module module) {
        this.module = module;
    }

    @Nullable
    public static BungeeCordModule getInstance(@NotNull Module module) {
        ModuleType moduleType = ModuleUtil.getModuleType(module);
        if (moduleType instanceof BungeeCordModuleType) {
            return map.computeIfAbsent(module, BungeeCordModule::new);
        }
        return null;
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    public VirtualFile getPluginYml() {
        return pluginYml;
    }

    public void setPluginYml(VirtualFile pluginYml) {
        this.pluginYml = pluginYml;
    }

    public Icon getIcon() {
        return PlatformAssets.BUNGEECORD_ICON;
    }
}
