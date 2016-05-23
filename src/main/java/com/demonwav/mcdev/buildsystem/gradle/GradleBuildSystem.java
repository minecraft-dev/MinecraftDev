package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.google.common.base.Strings;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import org.apache.commons.io.FileUtils;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.gradle.tooling.GradleConnector.newConnector;

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

                if (type == PlatformType.FORGE) {
                    // version http://files.minecraftforge.net/maven/net/minecraftforge/forge/json
                    // mappings http://export.mcpbot.bspk.rs/versions.json
                } else {
                    String buildGradleText = AbstractTemplate.applyBuildGradleTemplate(
                            module,
                            groupId,
                            version,
                            Strings.emptyToNull(configuration.description),
                            buildVersion
                    );

                    if (buildGradleText == null) {
                        return;
                    }

                    // Create the PSI file from the text, but don't write it until we are finished with it
                    PsiFile buildGradlePsi = PsiFileFactory.getInstance(module.getProject()).createFileFromText(GroovyLanguage.INSTANCE, buildGradleText);

                    if (buildGradlePsi == null) {
                        return;
                    }

                    // Write the repository and dependency data to the psi file
                    new WriteCommandAction.Simple(module.getProject(), buildGradlePsi) {
                        @Override
                        protected void run() throws Throwable {
                            buildGradlePsi.setName("build.gradle");
                            final GroovyFile groovyFile = (GroovyFile) buildGradlePsi;

                            // Add repositories
                            createRepositoriesOrDependencies(
                                    module.getProject(),
                                    groovyFile,
                                    "repositories",
                                    repositories.stream()
                                        .map(r -> String.format("maven {name = '%s'\nurl = '%s'\n}", r.getId(), r.getUrl()))
                                        .collect(Collectors.toList())
                            );

                            // Add dependencies
                            createRepositoriesOrDependencies(
                                    module.getProject(),
                                    groovyFile,
                                    "dependencies",
                                    dependencies.stream()
                                            .map(d -> String.format("compile '%s:%s:%s'", d.getGroupId(), d.getArtifactId(), d.getVersion()))
                                            .collect(Collectors.toList())
                            );

                            new ReformatCodeProcessor(buildGradlePsi, false).run();
                            PsiDirectory rootDirectoryPsi = PsiManager.getInstance(module.getProject()).findDirectory(rootDirectory);
                            if (rootDirectoryPsi != null) {
                                rootDirectoryPsi.add(buildGradlePsi);
                            }
                            buildGradle = rootDirectory.findChild("build.gradle");
                            if (buildGradle == null) {
                                return;
                            }

                            // Reformat the code to match their code style
                            PsiFile newBuildGradlePsi = PsiManager.getInstance(module.getProject()).findFile(buildGradle);
                            if (newBuildGradlePsi != null) {
                                new ReformatCodeProcessor(newBuildGradlePsi, false).run();
                            }
                        }
                    }.execute();

                    // Setup gradle wrapper
                    // We'll write the properties file to ensure it sets up with the right version
                    String wrapperDirPath = rootDirectory.createChildDirectory(this, "gradle").createChildDirectory(this, "wrapper").getPath();
                    FileUtils.writeLines(new File(wrapperDirPath, "gradle-wrapper.properties"), Collections.singletonList(
                            "distributionUrl=https\\://services.gradle.org/distributions/gradle-2.13-bin.zip"
                    ));

                    // Use gradle tooling to run the wrapper task
                    GradleConnector connector = newConnector();
                    connector.forProjectDirectory(new File(rootDirectory.getPath()));
                    ProjectConnection connection = connector.connect();
                    BuildLauncher launcher = connection.newBuild();
                    try {
                        launcher.forTasks("wrapper").run();
                    } finally {
                        connection.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createRepositoriesOrDependencies(Project project, GroovyFile file, String name, List<String> expressions) {
        // Get the block so we can start working with it
        GrClosableBlock block = getClosableBlockByName(file, name);

        if (block == null) {
            return;
        }

        // Create a super expression with all the expressions tied together
        String expressionText = expressions.stream().collect(Collectors.joining("\n"));

        // We can't create each expression and add them to the file...that won't work. Groovy requires a new line
        // from one method call expression to another, and there's no way to put whitespace in Psi because Psi is
        // stupid. So instead we make the whole thing as one big clump and insert it into the block
        GroovyFile fakeFile = GroovyPsiElementFactoryImpl.getInstance(project).createGroovyFile(expressionText, false, null);
        PsiElement last = block.getChildren()[block.getChildren().length - 1];
        block.addBefore(fakeFile, last);
    }

    @Nullable
    private GrClosableBlock getClosableBlockByName(PsiElement element, String name) {
        List<GrClosableBlock> blocks = getClosableBlocksByName(element, name);
        if (blocks.isEmpty()) {
            return null;
        } else {
            return blocks.get(0);
        }
    }

    @NotNull
    private List<GrClosableBlock> getClosableBlocksByName(PsiElement element, String name) {
        return Arrays.stream(element.getChildren())
                .filter(c -> {
                    // We want to find the child which has a GrReferenceExpression with the right name
                    return Arrays.stream(c.getChildren())
                            .filter(g -> g instanceof GrReferenceExpression && g.getText().equals(name))
                            .findAny().isPresent();
                }).map(c -> {
                    // We want to find the grandchild which is a GrCloseableBlock, this is the
                    // basis for the method block
                    return Arrays.stream(c.getChildren())
                            .filter(g -> g instanceof GrClosableBlock)
                            // cast to closable block so generics can handle this conversion
                            .map(g -> (GrClosableBlock) g)
                            .findFirst();
                }).filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
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
            // Get the gradle external task type, this is what set's it as a gradle task
            GradleExternalTaskConfigurationType gradleType = GradleExternalTaskConfigurationType.getInstance();
            // Create a gradle external system run config
            ExternalSystemRunConfiguration runConfiguration = new ExternalSystemRunConfiguration(
                    GradleConstants.SYSTEM_ID,
                    project,
                    gradleType.getConfigurationFactories()[0],
                    module.getName() + " build"
            );
            // Set relevant gradle values
            runConfiguration.getSettings().setExternalProjectPath(rootDirectory.getPath());
            runConfiguration.getSettings().setExecutionName(module.getName() + " build");
            runConfiguration.getSettings().setTaskNames(Collections.singletonList("build"));
            // Create a RunAndConfigurationSettings object, which defines general settings for the run configu
            RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(
                    RunManagerImpl.getInstanceImpl(project),
                    runConfiguration,
                    false
            );
            // Open the tool window and set it as a singleton run type
            settings.setActivateToolWindowBeforeRun(true);
            settings.setSingleton(true);

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

            String name = module.getName();
            // I can't find a way to read module group children, group path only goes up
            // So I guess check each module to see if it's a child....
            Collection<Module> children = Arrays.stream(ModuleManager.getInstance(project).getModules())
                    .filter(m -> {
                        String[] paths = ModuleManager.getInstance(project).getModuleGroupPath(m);
                        if (paths != null && paths.length > 0) {
                            if (name.equals(paths[0])) {
                                return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toList());

            // We need to check the parent too if it's a single module project
            children.add(module);
            ExternalProject externalRootProject = externalProjectDataCache.getRootExternalProject(GradleConstants.SYSTEM_ID, new File(rootDirectory.getCanonicalPath()));
            if (externalRootProject != null) {
                for (Module child : children) {
                    Map<String, ExternalSourceSet> externalSourceSets = externalProjectDataCache.findExternalProject(externalRootProject, child);

                    for (ExternalSourceSet sourceSet : externalSourceSets.values()) {
                        setupDirs(sourceDirectories, sourceSet, ExternalSystemSourceType.SOURCE);
                        setupDirs(resourceDirectories, sourceSet, ExternalSystemSourceType.RESOURCE);
                        setupDirs(testSourcesDirectories, sourceSet, ExternalSystemSourceType.TEST);
                        setupDirs(testResourceDirectories, sourceSet, ExternalSystemSourceType.TEST_RESOURCE);
                    }
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
                    ProjectConnection connection = null;
                    try {
                        connection = connector.connect();
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
                    } finally {
                        if (connection != null) {
                            connection.close();
                        }
                    }

                    // get repositories
                    repositories = new ArrayList<>();
                    // We need to climb the tree to get to the repositories

                    GrClosableBlock block = getClosableBlockByName(groovyFile, "repositories");
                    if (block != null) {
                        addRepositories(block);
                    }
                }
            }
        }
    }

    private void addRepositories(GrClosableBlock block) {
        List<GrClosableBlock> mavenBlocks = getClosableBlocksByName(block, "maven");
        if (mavenBlocks.isEmpty()) {
            return;
        }

        mavenBlocks.forEach(mavenBlock -> {
            BuildRepository repository = new BuildRepository();
            for (PsiElement child : mavenBlock.getChildren()) {
                if (child instanceof GrApplicationStatement) {
                    handleApplicationStatement((GrApplicationStatement) child, repository);
                } else if (child instanceof GrAssignmentExpression) {
                    handleAssignmentExpression((GrAssignmentExpression) child, repository);
                }
            }
            repositories.add(repository);
        });
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
