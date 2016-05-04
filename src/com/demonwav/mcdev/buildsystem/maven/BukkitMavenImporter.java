/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;
import com.demonwav.mcdev.platform.bukkit.BukkitProject;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

public class BukkitMavenImporter extends MinecraftMavenImporter {
    public BukkitMavenImporter() {
        super("org.bukkit", "bukkit");
    }

    public BukkitMavenImporter(String  groupId, String artifactId) {
        super(groupId, artifactId);
    }

    @NotNull
    @Override
    public BukkitModuleType getModuleType() {
        return BukkitModuleType.getInstance();
    }

    @Override
    public void resolve(Project project,
                        MavenProject mavenProject,
                        NativeMavenProjectHolder nativeMavenProject,
                        MavenEmbedderWrapper embedder,
                        ResolveContext context) throws MavenProcessCanceledException {
        super.resolve(project, mavenProject, nativeMavenProject, embedder, context);
        BukkitProject bukkitProject = BukkitProject.getInstance(project);
        bukkitProject.setPluginYml(project.getBaseDir().findFileByRelativePath("/src/main/resources/plugin.yml"));
        bukkitProject.getConfigManager();
    }
}
