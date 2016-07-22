/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.maven;

import com.demonwav.mcdev.buildsystem.maven.AbstractMavenImporter;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModule;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.util.Objects;

public class BungeeCordMavenImporter extends AbstractMavenImporter {

    public BungeeCordMavenImporter() {
        super(BungeeCordModuleType.getInstance());
    }

    @NotNull
    @Override
    public ModuleType getModuleType() {
        return JavaModuleType.getModuleType();
    }

    @Override
    public void resolve(Project project,
                        MavenProject mavenProject,
                        NativeMavenProjectHolder nativeMavenProject,
                        MavenEmbedderWrapper embedder,
                        ResolveContext context) throws MavenProcessCanceledException {

        super.resolve(project, mavenProject, nativeMavenProject, embedder, context);
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            // We'll make sure the project is setup
            if (Objects.equals(ModuleRootManager.getInstance(module).getContentRoots()[0], mavenProject.getFile().getParent())) {
                BungeeCordModule bungeeCordModule = MinecraftModule.getInstance(module, BungeeCordModuleType.getInstance());
                if (bungeeCordModule != null) {
                    bungeeCordModule.setPluginYml(project.getBaseDir().findFileByRelativePath("/src/main/resources/plugin.yml"));
                    return;
                }
            }
        }
    }
}
