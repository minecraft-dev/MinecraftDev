/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.maven;

import com.demonwav.bukkitplugin.BukkitModuleType;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

public abstract class Importer extends MavenImporter {

    protected final String GROUP_ID;
    protected final String ARTIFACT_ID;

    public Importer(String groupId, String artifactId) {
        super(groupId, artifactId);
        GROUP_ID = groupId;
        ARTIFACT_ID = artifactId;
    }

    @Override
    public void preProcess(Module module,
                           MavenProject mavenProject,
                           MavenProjectChanges changes,
                           MavenModifiableModelsProvider modifiableModelsProvider) {
    }

    @Override
    public void process(MavenModifiableModelsProvider modifiableModelsProvider,
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

    @NotNull
    @Override
    public BukkitModuleType getModuleType() {
        return BukkitModuleType.getInstance();
    }


}
