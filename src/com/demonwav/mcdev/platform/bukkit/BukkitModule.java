package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

public class BukkitModule extends AbstractModule {

    private static final Map<VirtualFile, BukkitModule> map = new HashMap<>();

    private VirtualFile pluginYml;
    @NotNull
    private Module module;
    private PlatformType type;
    private PluginConfigManager configManager;

    private BuildSystem buildSystem;

    private BukkitModule(@NotNull Module module, @NotNull BukkitModuleType type) {
        this.type = type.getPlatformType();
        this.module = module;
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module, type.getPlatformType());

            if (buildSystem.getResourceDirectory() != null) {
                pluginYml = buildSystem.getResourceDirectory().findChild("plugin.yml");

                if (pluginYml != null) {
                    this.configManager = new PluginConfigManager(this);
                }
            }
        }
    }

    private BukkitModule(@NotNull Module module, @NotNull BukkitModuleType type, @Nullable BuildSystem buildSystem) {
        this.type = type.getPlatformType();
        this.module = module;
        this.buildSystem = buildSystem;
        if (buildSystem == null) {
            // set up build system
            buildSystem = BuildSystem.getInstance(module);
            if (buildSystem != null) {
                // a valid build system was detected, import it
                buildSystem.reImport(module, type.getPlatformType());
            }
        }
        // it may still be null if there was no valid build system detected
        if (buildSystem != null && buildSystem.getResourceDirectory() != null) {
            this.pluginYml = buildSystem.getResourceDirectory().findChild("plugin.yml");
            if (pluginYml != null) {
                this.configManager = new PluginConfigManager(this);
            }
        }
    }

    @Nullable
    public static BukkitModule getInstance(@NotNull Module module) {
        VirtualFile moduleRoot = LocalFileSystem.getInstance().findFileByPath(ModuleUtil.getModuleDirPath(module));
        ModuleType moduleType = ModuleUtil.getModuleType(module);
        // order matters here
        if (moduleType instanceof BukkitModuleType) {
            if (moduleType instanceof SpigotModuleType) {
                if (module instanceof PaperModuleType) {
                    return map.computeIfAbsent(moduleRoot, m -> new BukkitModule(module, PaperModuleType.getInstance()));
                }
                return map.computeIfAbsent(moduleRoot, m -> new BukkitModule(module, SpigotModuleType.getInstance()));
            }
            return map.computeIfAbsent(moduleRoot, m -> new BukkitModule(module, BukkitModuleType.getInstance()));
        }

        return null;
    }

    @Nullable
    public PluginConfigManager getConfigManager() {
        if (configManager == null) {
            if (pluginYml != null) {
                configManager = new PluginConfigManager(this);
            }
        }
        return configManager;
    }

    @NotNull
    public Module getModule() {
        return module;
    }

    public VirtualFile getPluginYml() {
        return pluginYml;
    }

    public void setPluginYml(@NotNull VirtualFile pluginYml) {
        this.pluginYml = pluginYml;
    }

    public Icon getIcon() {
        return type.getType().getIcon();
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public PlatformType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BukkitModule that = (BukkitModule) o;

        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return module.hashCode();
    }

    public static BukkitModule setType(@NotNull Module module, @NotNull BukkitModuleType type) {
        VirtualFile moduleRoot = LocalFileSystem.getInstance().findFileByPath(ModuleUtil.getModuleDirPath(module));
        return map.compute(moduleRoot, (m, b) -> new BukkitModule(module, type, null));
    }

    public static BukkitModule setBuildSystem(@NotNull Module module, @NotNull BukkitModuleType type, @NotNull BuildSystem buildSystem) {
        VirtualFile moduleRoot = LocalFileSystem.getInstance().findFileByPath(ModuleUtil.getModuleDirPath(module));
        return map.compute(moduleRoot, (m, b) -> new BukkitModule(module, type, buildSystem));
    }
}
