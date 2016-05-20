package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.HashMap;
import java.util.Map;

public class BukkitModule extends AbstractModule {

    private static final Map<VirtualFile, BukkitModule> map = new HashMap<>();

    private VirtualFile pluginYml;
    private PlatformType type;
    private PluginConfigManager configManager;
    private final BukkitModuleType moduleType;

    BukkitModule(@NotNull Module module, @NotNull BukkitModuleType type) {
        this.moduleType = type;
        this.type = type.getPlatformType();
        this.module = module;
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module, type.getPlatformType());
            pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);

            if (pluginYml != null) {
                this.configManager = new PluginConfigManager(this);
            }
        }
    }

    BukkitModule(@NotNull Module module, @NotNull BukkitModuleType type, @Nullable BuildSystem buildSystem) {
        this.moduleType = type;
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
        if (buildSystem != null) {
            this.pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);
            if (pluginYml != null) {
                this.configManager = new PluginConfigManager(this);
            }
        }
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

    public VirtualFile getPluginYml() {
        return pluginYml;
    }

    @Override
    public Icon getIcon() {
        return type.getType().getIcon();
    }

    @Override
    public MinecraftModuleType getModuleType() {
        return moduleType;
    }

    @Override
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

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return "org.bukkit.event.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass) {
        return "Parameter is not a subclass of org.bukkit.event.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }
}
