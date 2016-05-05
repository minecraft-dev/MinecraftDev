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

import com.intellij.ide.FileIconProvider;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.util.List;
import java.util.Map;

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
        BukkitProject bukkitProject = BukkitProject.getInstance(project); // We'll make sure the project is setup
        if (bukkitProject != null) {
            bukkitProject.getConfigManager(); // add config watcher
        }
    }


}
