package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.platform.bukkit.yaml.PluginConfigManager;
import com.demonwav.mcdev.platform.AbstractProject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BukkitProject extends AbstractProject {

    private static final Map<Project, BukkitProject> map = new HashMap<>();

    private VirtualFile pluginYml;
    @NotNull
    private Project project;

    private PluginConfigManager configManager;

    private BukkitProject(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static BukkitProject getInstance(@NotNull Project project) {
        BukkitProject result;
        if (!map.containsKey(project)) {
            result = new BukkitProject(project);
            map.put(project, result);
        } else {
            return map.get(project);
        }
        return result;
    }

    @NotNull
    public PluginConfigManager getConfigManager() {
        if (configManager == null)
            configManager = new PluginConfigManager(this);
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
}
