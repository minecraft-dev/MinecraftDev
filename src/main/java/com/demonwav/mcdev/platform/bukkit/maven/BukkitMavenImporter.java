package com.demonwav.mcdev.platform.bukkit.maven;

import com.demonwav.mcdev.buildsystem.maven.AbstractMavenImporter;
import com.demonwav.mcdev.platform.PlatformUtil;
import com.demonwav.mcdev.platform.bukkit.BukkitModuleType;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.util.Objects;

public class BukkitMavenImporter extends AbstractMavenImporter {

    public BukkitMavenImporter() {
        super(BukkitModuleType.getInstance());
    }

    public BukkitMavenImporter(@NotNull final BukkitModuleType type) {
        super(type);
    }

    @NotNull
    @Override
    public BukkitModuleType getModuleType() {
        return BukkitModuleType.getInstance();
    }

    @Override
    public void resolve(Project project, MavenProject mavenProject, NativeMavenProjectHolder nativeMavenProject, MavenEmbedderWrapper embedder, ResolveContext context) throws MavenProcessCanceledException {
        super.resolve(project, mavenProject, nativeMavenProject, embedder, context);
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // We'll make sure the project is setup
            if (Objects.equals(LocalFileSystem.getInstance().findFileByPath(ModuleRootManager.getInstance(module).getContentRoots()[0].getPath()), mavenProject.getFile().getParent())) {
                PlatformUtil.getInstance(module);
            }
        }
    }
}
