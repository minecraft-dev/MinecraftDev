package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.MinecraftModuleType;

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.project.ResolveContext;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractMavenImporter extends MavenImporter {

    @NotNull
    protected final AbstractModuleType type;

    public AbstractMavenImporter(@NotNull final AbstractModuleType type) {
        super(type.getGroupId(), type.getArtifactId());
        this.type = type;
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
        postTasks.add((project, embeddersManager, console, indicator) ->
            MavenProjectsManager.getInstance(module.getProject()).addManagedFilesOrUnignore(Collections.singletonList(mavenProject.getFile()))
        );
    }

    @Override
    public boolean isApplicable(MavenProject mavenProject) {
        return !mavenProject.findDependencies(type.getGroupId(), type.getArtifactId()).isEmpty();
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
                Optional.ofNullable(BuildSystem.getInstance(module)).ifPresent(buildSystem -> buildSystem.reImport(module).done(b -> {
                    MinecraftModuleType.addOption(module, type.getId());
                    // We want to make sure the project "knows" about this change

                    MinecraftModule.getInstance(module);
                }));
            }
        }
    }
}
