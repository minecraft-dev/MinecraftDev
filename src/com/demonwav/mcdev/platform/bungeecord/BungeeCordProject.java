package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.platform.AbstractProject;
import com.demonwav.mcdev.platform.bukkit.BukkitProject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

public class BungeeCordProject extends AbstractProject {

    private static final Map<Project, BungeeCordProject> map = new HashMap<>();

    private Icon icon;
    @NotNull
    private Project project;

    private VirtualFile pluginYml;

    private BungeeCordProject(@NotNull Project project) {
        this.project = project;
    }


    @NotNull
    public static BungeeCordProject getInstance(@NotNull Project project) {
        return map.computeIfAbsent(project, BungeeCordProject::new);
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    public VirtualFile getPluginYml() {
        return pluginYml;
    }

    public void setPluginYml(VirtualFile pluginYml) {
        this.pluginYml = pluginYml;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }
}
