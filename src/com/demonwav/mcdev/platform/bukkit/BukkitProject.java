package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;
import com.demonwav.mcdev.platform.AbstractProject;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

public class BukkitProject extends AbstractProject {

    private static final Map<Project, BukkitProject> map = new HashMap<>();

    private VirtualFile pluginYml;
    @NotNull
    private Project project;
    private PlatformType type;
    private PluginConfigManager configManager;

    private BuildSystem buildSystem;

    private Icon icon;

    private BukkitProject(@NotNull Project project, @NotNull BukkitModuleType type) {
        this.project = project;
        this.type = type.getPlatformType();
        this.icon = type.getIcon();
        buildSystem = BuildSystem.getInstance(project).reImport(project, type.getPlatformType());
        if (buildSystem.getResourceDirectory() != null) {
            pluginYml = buildSystem.getResourceDirectory().findChild("plugin.yml");
            this.configManager = new PluginConfigManager(this);
        }
    }

    private BukkitProject(@NotNull Project project, @NotNull BukkitModuleType type, @NotNull BuildSystem buildSystem) {
        this.project = project;
        this.type = type.getPlatformType();
        this.icon = type.getIcon();
        this.buildSystem = buildSystem;
        this.pluginYml = buildSystem.getResourceDirectory().findChild("plugin.yml");
        this.configManager = new PluginConfigManager(this);
    }

    @Nullable
    public static BukkitProject getInstance(@NotNull Project project) {
        // TODO: support multiple modules
        // lol wat is even this method
        if (ModuleUtil.hasModulesOfType(project, BukkitModuleType.getInstance())) {
            BukkitModuleType type = (BukkitModuleType) ModuleUtil.getModuleType(ModuleUtil.getModulesOfType(project, BukkitModuleType.getInstance()).iterator().next());
            if (type != null) {
                return map.computeIfAbsent(project, p -> new BukkitProject(p, type));
            } else {
                return null;
            }
        }

        if (ModuleUtil.hasModulesOfType(project, SpigotModuleType.getInstance())) {
            SpigotModuleType type = (SpigotModuleType) ModuleUtil.getModuleType(ModuleUtil.getModulesOfType(project, SpigotModuleType.getInstance()).iterator().next());
            if (type != null) {
                return map.computeIfAbsent(project, p -> new BukkitProject(p, type));
            } else {
                return null;
            }
        }

        if (ModuleUtil.hasModulesOfType(project, PaperModuleType.getInstance())) {
            PaperModuleType type = (PaperModuleType) ModuleUtil.getModuleType(ModuleUtil.getModulesOfType(project, PaperModuleType.getInstance()).iterator().next());
            if (type != null) {
                return map.computeIfAbsent(project, p -> new BukkitProject(p, type));
            } else {
                return null;
            }
        }
        return null;
    }

    @NotNull
    public PluginConfigManager getConfigManager() {
        if (configManager == null) {
            configManager = new PluginConfigManager(this);
        }
        return configManager;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public VirtualFile getPluginYml() {
        return pluginYml;
    }

    public void setPluginYml(@NotNull VirtualFile pluginYml) {
        this.pluginYml = pluginYml;
    }

    public void setIcon(@NotNull Icon icon) {
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
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

        BukkitProject that = (BukkitProject) o;

        return project.equals(that.project);

    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }

    public static BukkitProject set(@NotNull Project project, @NotNull BukkitModuleType type, @NotNull BuildSystem buildSystem) {
        return map.computeIfAbsent(project, p -> new BukkitProject(p, type, buildSystem));
    }
}
