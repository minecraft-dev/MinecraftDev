/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven;

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.idea.maven.importing.MavenImporter;
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

public abstract class MinecraftMavenImporter extends MavenImporter {

    protected final String GROUP_ID;
    protected final String ARTIFACT_ID;

    public MinecraftMavenImporter(String groupId, String artifactId) {
        super(groupId, artifactId);
        GROUP_ID = groupId;
        ARTIFACT_ID = artifactId;
    }

    @Override
    public void preProcess(Module module,
                           MavenProject mavenProject,
                           MavenProjectChanges changes,
                           IdeModifiableModelsProvider modifiableModelsProvider) {
    }

    @Override
    public void process(IdeModifiableModelsProvider modifiableModelsProvider,
                        Module module,
                        MavenRootModelAdapter rootModel,
                        MavenProjectsTree mavenModel,
                        MavenProject mavenProject,
                        MavenProjectChanges changes,
                        Map<MavenProject, String> mavenProjectToModuleName,
                        List<MavenProjectsProcessorTask> postTasks) {
    }

    @Override
    public boolean isApplicable(MavenProject mavenProject) {
        return !mavenProject.findDependencies(GROUP_ID, ARTIFACT_ID).isEmpty();
    }

    @Override
    public void resolve(Project project,
                        MavenProject mavenProject,
                        NativeMavenProjectHolder nativeMavenProject,
                        MavenEmbedderWrapper embedder,
                        ResolveContext context) throws MavenProcessCanceledException {
        super.resolve(project, mavenProject, nativeMavenProject, embedder, context);
    }
}
