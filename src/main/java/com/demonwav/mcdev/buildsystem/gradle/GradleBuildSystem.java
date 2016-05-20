package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.google.common.base.Strings;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.gradle.api.invocation.Gradle;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRuntimeConfigurationProducer;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunConfiguration;
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunConfigurationType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleBuildSystem extends BuildSystem {

    final private static Pattern pattern = Pattern.compile("id='GradleModuleVersion\\{group='([a-zA-Z_\\-\\d\\.]+)', name='([a-zA-Z_\\-\\d\\.]+)', version='([a-zA-Z_\\-\\d\\.]+)'\\}'");

    @Nullable
    private VirtualFile buildGradle;

    @Override
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        rootDirectory.refresh(false, true);
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                sourceDirectories = Collections.singletonList(VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/java"));
                resourceDirectories = Collections.singletonList(VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/resources"));
                testSourcesDirectories = Collections.singletonList(VfsUtil.createDirectories(rootDirectory.getPath() + "/src/test/java"));
                testResourceDirectories = Collections.singletonList(VfsUtil.createDirectories(rootDirectory.getPath() + "/src/test/resources"));
                buildGradle = rootDirectory.findOrCreateChildData(this, "build.gradle");

                if (type == PlatformType.FORGE) {
                    // version http://files.minecraftforge.net/maven/net/minecraftforge/forge/json
                    // mappings http://export.mcpbot.bspk.rs/versions.json

                } else {
                    AbstractTemplate.applyBuildGradleTemplate(
                            module,
                            buildGradle,
                            groupId,
                            version,
                            Strings.emptyToNull(configuration.description),
                            buildVersion,
                            // TODO: use psi instead of templates to allow all repos and dependencies to be added dynamically
                            repositories.get(0).getId(),
                            repositories.get(0).getUrl(),
                            dependencies.get(0).getGroupId(),
                            dependencies.get(0).getArtifactId(),
                            dependencies.get(0).getVersion()
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void finishSetup(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        Project project = module.getProject();

        // Tell Gradle to import this project
        final ProjectDataManager projectDataManager = ServiceManager.getService(ProjectDataManager.class);
        GradleProjectImportBuilder gradleProjectImportBuilder = new GradleProjectImportBuilder(projectDataManager);
        final GradleProjectImportProvider gradleProjectImportProvider = new GradleProjectImportProvider(gradleProjectImportBuilder);
        if (buildGradle != null) {
            AddModuleWizard wizard = new AddModuleWizard(project, buildGradle.getPath(), gradleProjectImportProvider);
            if (wizard.showAndGet()) {
                ImportModuleAction.createFromWizard(project, wizard);
            }

            // Set up the run config
            RunnerAndConfigurationSettings settings = RunManager.getInstance(project)
                    .createRunConfiguration(
                            module.getName() + " Build",
                            GroovyScriptRunConfigurationType.getInstance().getConfigurationFactories()[0]
                    );
            GroovyScriptRunConfiguration runConfiguration = (GroovyScriptRunConfiguration) settings.getConfiguration();
            // Set some general settings
            runConfiguration.setModule(module);
            runConfiguration.setScriptPath(buildGradle.getCanonicalPath());
            runConfiguration.setWorkingDirectory(getRootDirectory().getCanonicalPath());
            runConfiguration.setScriptParameters("build");

            settings.setActivateToolWindowBeforeRun(true);
            settings.setSingleton(true);

            // FIXME this always puts "make" in the run before thing, which we don't want
            // I've tried this, I've tried giving it an empty list, I've tried setting it to null, I don't know what to
            // try at this point

            // I also tried this, which prevents the make build to run, but doesn't remove it, so that also isn't optimal
//            GroovyScriptRunConfiguration runConfiguration = new GroovyScriptRunConfiguration(
//                    module.getName() + " build",
//                    project,
//                    GroovyScriptRunConfigurationType.getInstance().getConfigurationFactories()[0]
//            ) {
//                @Override
//                public boolean excludeCompileBeforeLaunchOption() {
//                    return true;
//                }
//            };
//
//            RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(
//                    RunManagerImpl.getInstanceImpl(project), runConfiguration, false
//            );

            List<BeforeRunTask> tasks = RunManagerEx.getInstanceEx(project).getBeforeRunTasks(runConfiguration);
            tasks = tasks.stream().peek(t -> {
                if (t instanceof CompileStepBeforeRun.MakeBeforeRunTask) {
                    t.setEnabled(false);
                }
            }).collect(Collectors.toList());
            RunManagerEx.getInstanceEx(project).setBeforeRunTasks(runConfiguration, tasks, true);

            // Apply the run config and select it
            RunManager.getInstance(project).addConfiguration(settings, false);
            RunManager.getInstance(project).setSelectedConfiguration(settings);

        }
    }

    @Override
    public void reImport(@NotNull Module module, @NotNull PlatformType type) {
        Project project = module.getProject();

        // root directory is the first content root
        rootDirectory = ModuleRootManager.getInstance(module).getContentRoots()[0];

        if (rootDirectory.getCanonicalPath() != null) {
            sourceDirectories = new ArrayList<>();
            resourceDirectories = new ArrayList<>();
            testSourcesDirectories = new ArrayList<>();
            testResourceDirectories = new ArrayList<>();

            ExternalProjectDataCache externalProjectDataCache = ExternalProjectDataCache.getInstance(project);
            assert project.getBasePath() != null;
            ExternalProject externalRootProject = externalProjectDataCache.getRootExternalProject(GradleConstants.SYSTEM_ID, new File(project.getBasePath()));
            if (externalRootProject != null) {
                Map<String, ExternalSourceSet> externalSourceSets = externalProjectDataCache.findExternalProject(externalRootProject, module);

                for (ExternalSourceSet sourceSet : externalSourceSets.values()) {
                    setupDirs(sourceDirectories, sourceSet, ExternalSystemSourceType.SOURCE);
                    setupDirs(resourceDirectories, sourceSet, ExternalSystemSourceType.RESOURCE);
                    setupDirs(testSourcesDirectories, sourceSet, ExternalSystemSourceType.TEST);
                    setupDirs(testResourceDirectories, sourceSet, ExternalSystemSourceType.TEST_RESOURCE);
                }

                groupId = externalRootProject.getGroup();
                artifactId = externalRootProject.getName();
                version = externalRootProject.getVersion();

                pluginName = externalRootProject.getName();
            }

            VirtualFile file = rootDirectory.findFileByRelativePath("build.gradle");
            if (file != null) {
                GroovyFile groovyFile = (GroovyFile) PsiManager.getInstance(project).findFile(file);
                if (groovyFile != null) {
                    // get dependencies
                    dependencies = new ArrayList<>();

                    GradleConnector connector = GradleConnector.newConnector();
                    connector.forProjectDirectory(new File(rootDirectory.getCanonicalPath()));
                    ProjectConnection connection = connector.connect();
                    IdeaProject ideaProject = connection.getModel(IdeaProject.class);
                    for (IdeaModule ideaModule : ideaProject.getModules()) {
                        for (IdeaDependency ideaDependency : ideaModule.getDependencies()) {
                            String version = ideaDependency.toString();
                            Matcher matcher = pattern.matcher(version);
                            if (matcher.find()) {
                                String tempGroupId = matcher.group(1);
                                String tempArtifactId = matcher.group(2);
                                String tempVersion = matcher.group(3);
                                String scope = ideaDependency.getScope().getScope().toLowerCase();

                                dependencies.add(new BuildDependency(tempGroupId, tempArtifactId, tempVersion, scope));
                            }
                        }
                    }

                    // get build version
                    buildVersion = ideaProject.getLanguageLevel().getLevel();
                    buildVersion = buildVersion.replaceAll("JDK_", "").replaceAll("_", ".");

                    connection.close();

                    // get repositories
                    repositories = new ArrayList<>();
                    for (PsiElement element : groovyFile.getChildren()) {
                        if (element instanceof GrMethodCallExpression) {
                            GrMethodCallExpression expression = (GrMethodCallExpression) element;
                            for (PsiElement child : expression.getChildren()) {
                                if (child instanceof GrReferenceExpression) {
                                    GrReferenceExpression referenceExpression = (GrReferenceExpression) child;
                                    if (referenceExpression.getText().equals("repositories")) {
                                        while (!(child instanceof GrClosableBlock)) {
                                            child = child.getNextSibling();
                                            if (child == null) {
                                                break;
                                            }
                                        }
                                        if (child != null) {
                                            addRepositories((GrClosableBlock) child);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addRepositories(GrClosableBlock block) {
        for (PsiElement element : block.getChildren()) {
            if (element instanceof GrMethodCallExpression) {
                GrMethodCallExpression methodCallExpression = (GrMethodCallExpression) element;
                for (PsiElement psiElement : methodCallExpression.getChildren()) {
                    if (psiElement instanceof GrReferenceExpression) {
                        GrReferenceExpression referenceExpression = (GrReferenceExpression) psiElement;
                        if (referenceExpression.getText().equals("maven")) {
                            while (!(psiElement instanceof GrClosableBlock)) {
                                psiElement = psiElement.getNextSibling();
                                if (psiElement == null) {
                                    break;
                                }
                            }
                            if (psiElement != null) {
                                BuildRepository repository = new BuildRepository();
                                for (PsiElement child : psiElement.getChildren()) {
                                    if (child instanceof GrApplicationStatement) {
                                        handleApplicationStatement((GrApplicationStatement) child, repository);
                                    } else if (child instanceof GrAssignmentExpression) {
                                        handleAssignmentExpression((GrAssignmentExpression) child, repository);
                                    }
                                }
                                repositories.add(repository);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleApplicationStatement(GrApplicationStatement statement, BuildRepository repository) {
        GrCommandArgumentList list = statement.getArgumentList();
        if (list.getChildren().length > 0) {
            if (statement.getInvokedExpression().getText().equals("name")) {
                repository.setId(list.getChildren()[0].getText().replaceAll("'", ""));
            } else if (statement.getInvokedExpression().getText().equals("url")) {
                repository.setUrl(list.getChildren()[0].getText().replaceAll("'", ""));
            }
        }
    }

    private void handleAssignmentExpression(GrAssignmentExpression expression, BuildRepository repository) {
        if (expression.getLValue().getText().equals("name")) {
            if (expression.getRValue() != null) {
                repository.setId(expression.getRValue().getText().replaceAll("'", ""));
            }
        } else if (expression.getLValue().getText().equals("url")) {
            if (expression.getRValue() != null) {
                repository.setUrl(expression.getRValue().getText().replaceAll("'", ""));
            }
        }
    }

    private void setupDirs(List<VirtualFile> directories, ExternalSourceSet set, ExternalSystemSourceType type) {
        if (set.getSources().get(type) != null) {
            set.getSources().get(type).getSrcDirs().forEach(dir ->
                    directories.add(LocalFileSystem.getInstance().findFileByPath(dir.getAbsolutePath()))
            );
        }
    }
}
