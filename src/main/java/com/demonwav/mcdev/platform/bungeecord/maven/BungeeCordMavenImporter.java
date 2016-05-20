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
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformUtil;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModule;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
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
    public BungeeCordModuleType getModuleType() {
        return BungeeCordModuleType.getInstance();
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
            if (Objects.equals(LocalFileSystem.getInstance().findFileByPath(ModuleUtil.getModuleDirPath(module)), mavenProject.getFile().getParent())) {
                AbstractModule bungeeCordModule = PlatformUtil.getInstance(module);
                if (bungeeCordModule instanceof BungeeCordModule) {
                    ((BungeeCordModule) bungeeCordModule).setPluginYml(project.getBaseDir().findFileByRelativePath("/src/main/resources/plugin.yml"));
                    break;
                }
            }
        }
    }
}
