package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@SuppressWarnings("unused")
public class BukkitModule<T extends BukkitModuleType> extends AbstractModule {

    private VirtualFile pluginYml;
    private PlatformType type;
    private PluginConfigManager configManager;
    private T moduleType;

    BukkitModule(@NotNull Module module, @NotNull T type) {
        this.moduleType = type;
        this.type = type.getPlatformType();
        this.module = module;
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            if (!buildSystem.isImported()) {
                buildSystem.reImport(module).done(buildSystem  -> setup());
            } else {
                if (buildSystem.isFinishImport()) {
                    setup();
                }
            }
        }
    }

    private void setup() {
        pluginYml = buildSystem.findFile("plugin.yml", SourceType.RESOURCE);

        if (pluginYml != null) {
            this.configManager = new PluginConfigManager(this);
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
        if (buildSystem == null) {
            buildSystem = BuildSystem.getInstance(module);
        }
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
    public T getModuleType() {
        return moduleType;
    }

    private void setModuleType(@NotNull T moduleType) {
        this.moduleType = moduleType;
    }

    @Override
    public PlatformType getType() {
        return type;
    }

    private void setType(@NotNull PlatformType type) {
        this.type = type;
    }

    @Override
    public boolean isEventClassValid(PsiClass eventClass, PsiMethod method) {
        return "org.bukkit.event.Event".equals(eventClass.getQualifiedName());
    }

    @Override
    public String writeErrorMessageForEventParameter(PsiClass eventClass, PsiMethod method) {
        return "Parameter is not a subclass of org.bukkit.event.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BukkitModule<?> that = (BukkitModule<?>) o;
        return Objects.equal(pluginYml, that.pluginYml) &&
            type == that.type &&
            Objects.equal(configManager, that.configManager) &&
            Objects.equal(moduleType, that.moduleType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pluginYml, type, configManager, moduleType);
    }
}
