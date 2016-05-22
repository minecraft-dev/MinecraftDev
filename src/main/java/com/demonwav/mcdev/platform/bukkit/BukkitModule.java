package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class BukkitModule extends AbstractModule {

    private VirtualFile pluginYml;
    private PlatformType type;
    private PluginConfigManager configManager;
    private BukkitModuleType moduleType;

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

    private BukkitModule(@NotNull Module module, @NotNull BukkitModuleType type, @Nullable BuildSystem buildSystem) {
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
        if (pluginYml == null && buildSystem != null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);
        }
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

    private void setModuleType(BukkitModuleType moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public PlatformType getType() {
        return type;
    }

    private void setType(PlatformType type) {
        this.type = type;
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
