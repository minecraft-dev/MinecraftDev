package com.demonwav.mcdev;

import com.demonwav.mcdev.pluginyaml.PluginConfigManager;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class BukkitProject {

    private static final Map<Project, BukkitProject> map = new HashMap<>();

    @Getter @Setter private VirtualFile pluginYml;
    @Getter @Setter private Project project;

    private PluginConfigManager configManager;

    private BukkitProject(Project project) {
        this.project = project;
    }

    public static BukkitProject getInstance(Project project) {
        BukkitProject result;
        if (!map.containsKey(project)) {
            result = new BukkitProject(project);
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
