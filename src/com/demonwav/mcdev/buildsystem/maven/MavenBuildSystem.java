package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.maven.pom.Dependency;
import com.demonwav.mcdev.buildsystem.maven.pom.MavenProject;
import com.demonwav.mcdev.buildsystem.maven.pom.Repository;
import com.demonwav.mcdev.platform.AbstractTemplate;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MavenBuildSystem extends BuildSystem {

    private String artifactId;
    private String groupId;
    private String version;
    private List<BuildDependency> dependencies;
    private List<BuildRepository> repositories;
    private VirtualFile rootDirectory;

    private VirtualFile sourceDirectory;
    private VirtualFile resourceDirectory;
    private VirtualFile testDirectory;
    private VirtualFile pomFile;

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public void setPluginName(@NotNull String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public void setArtifactId(@NotNull String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(@NotNull String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(@NotNull String version) {
        this.version = version;
    }

    @Override
    public List<BuildDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public void setDependencies(@NotNull List<BuildDependency> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public List<BuildRepository> getRepositories() {
        return repositories;
    }

    @Override
    public void setRepositories(@NotNull List<BuildRepository> repositories) {
        this.repositories = repositories;
    }

    @Override
    public VirtualFile getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public void setRootDirectory(@NotNull VirtualFile rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public VirtualFile getSourceDirectory() {
        return sourceDirectory;
    }

    @Override
    public VirtualFile getResourceDirectory() {
        return resourceDirectory;
    }

    @Override
    public VirtualFile getTestDirectory() {
        return testDirectory;
    }

    public VirtualFile getPomFile() {
        return pomFile;
    }

    @Override
    public void create(@NotNull Project project) {
        rootDirectory.refresh(false, true);
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                sourceDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/java");
                resourceDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/resources");
                testDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/test/java");

                PsiFile pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, AbstractTemplate.applyPomTemplate(project, buildVersion));
                pomPsi.setName("pom.xml");

                XmlFile pomXmlPsi = (XmlFile) pomPsi;
                new WriteCommandAction.Simple(project, pomPsi) {
                    @Override
                    protected void run() throws Throwable {
                        XmlTag root = pomXmlPsi.getRootTag();

                        DomManager manager = DomManager.getDomManager(project);
                        MavenProject mavenProject = manager.getFileElement(pomXmlPsi, MavenProject.class, "project").getRootElement();

                        mavenProject.getGroupId().setValue(groupId);
                        mavenProject.getArtifactId().setValue(artifactId);
                        mavenProject.getVersion().setValue(version);
                        mavenProject.getName().setValue(pluginName);

                        assert root != null;
                        XmlTag properties = root.findFirstSubTag("properties");
                        assert properties != null;
                        XmlTag buildProperty = properties.findFirstSubTag("project.build.sourceEncoding");

                        if (pluginAuthor != null && !pluginAuthor.trim().isEmpty()) {
                            XmlTag authorTag = properties.createChildTag("project.author", null, pluginAuthor, false);
                            properties.addAfter(authorTag, buildProperty);
                        }

                        for (BuildRepository buildRepository : repositories) {
                            Repository repository = mavenProject.getRepositories().addRepository();
                            repository.getId().setValue(buildRepository.getId());
                            repository.getUrl().setValue(buildRepository.getUrl());
                        }

                        for (BuildDependency buildDependency : dependencies) {
                            Dependency dependency = mavenProject.getDependencies().addDependency();
                            dependency.getGroupId().setValue(buildDependency.getGroupId());
                            dependency.getArtifactId().setValue(buildDependency.getArtifactId());
                            dependency.getVersion().setValue(buildDependency.getVersion());
                            dependency.getScope().setValue(buildDependency.getScope());
                        }

                        PsiDirectory rootDirectoryPsi = PsiManager.getInstance(project).findDirectory(rootDirectory);
                        if (rootDirectoryPsi != null) {
                            rootDirectoryPsi.add(pomPsi);
                        }

                        pomFile = rootDirectory.findChild("pom.xml");
                        // Reformat the code to match their code style
                        PsiFile pomFilePsi = PsiManager.getInstance(project).findFile(pomFile);
                        if (pomFilePsi != null) {
                            new ReformatCodeProcessor(pomFilePsi, false).run();
                        }
                    }
                }.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void finishSetup(@NotNull Project project) {
        // Force Maven to setup the project
        MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();

        // Setup the default Maven run config
        if (getRootDirectory().getCanonicalPath() != null) {
            MavenRunnerParameters params = new MavenRunnerParameters();
            params.setWorkingDirPath(getRootDirectory().getCanonicalPath());
            params.setGoals(Arrays.asList("clean", "package"));
            RunnerAndConfigurationSettings runnerSettings = MavenRunConfigurationType.createRunnerAndConfigurationSettings(null, null, params, project);
            runnerSettings.setName("clean package");
            RunManager.getInstance(project).addConfiguration(runnerSettings, true);
            RunManager.getInstance(project).setSelectedConfiguration(runnerSettings);
        }
    }

    @Override
    public void reImport() {

    }
}
