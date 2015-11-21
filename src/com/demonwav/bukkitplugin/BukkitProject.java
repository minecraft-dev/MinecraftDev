package com.demonwav.bukkitplugin;

import com.demonwav.bukkitplugin.pluginyaml.PluginConfigManager;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class BukkitProject {

    private static final Map<Project, BukkitProject> map = new HashMap<>();

    @Getter @Setter private VirtualFile pluginYml;
    @Getter @Setter private Project project;

    private PluginConfigManager configManager;

    public static BukkitProject getInstance(Project project) {
        BukkitProject result;
        if (!map.containsKey(project)) {
            result = new BukkitProject();
            map.put(project, result);
        } else {
            return map.get(project);
        }
        return result;
    }

    public PluginConfigManager getConfigManager() {
        if (configManager == null)
            configManager = new PluginConfigManager(this);
        return configManager;
    }
}
