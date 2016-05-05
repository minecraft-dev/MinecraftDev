package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.maven.pom.Dependency;
import com.demonwav.mcdev.buildsystem.maven.pom.MavenProjectXml;
import com.demonwav.mcdev.buildsystem.maven.pom.Repository;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.bukkit.BukkitTemplate;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordTemplate;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
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
import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MavenBuildSystem extends BuildSystem {

    private VirtualFile pomFile;

    public VirtualFile getPomFile() {
        return pomFile;
    }

    @Override
    public void create(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        // TODO: this only supports Bukkit's and BungeeCord's pom right now, need to add Sponge pom support as well
        rootDirectory.refresh(false, true);
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                sourceDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/java");
                resourceDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/main/resources");
                testSourcesDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/test/java");
                testResourceDirectory = VfsUtil.createDirectories(rootDirectory.getPath() + "/src/test/resources");

                PsiFile pomPsi = null;

                if (type == PlatformType.BUKKIT || type == PlatformType.SPIGOT || type == PlatformType.PAPER) {
                    pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, BukkitTemplate.applyPomTemplate(project, buildVersion));
                } else if (type == PlatformType.BUNGEECORD) {
                    pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, BungeeCordTemplate.applyPomTemplate(project, buildVersion));
                } // TODO: Sponge
                pomPsi.setName("pom.xml");

                final XmlFile pomXmlPsi = (XmlFile) pomPsi;
                final PsiFile finalPomPsi = pomPsi;
                new WriteCommandAction.Simple(project, pomPsi) {
                    @Override
                    protected void run() throws Throwable {
                        XmlTag root = pomXmlPsi.getRootTag();

                        DomManager manager = DomManager.getDomManager(project);
                        MavenProjectXml mavenProjectXml = manager.getFileElement(pomXmlPsi, MavenProjectXml.class, "project").getRootElement();

                        mavenProjectXml.getGroupId().setValue(groupId);
                        mavenProjectXml.getArtifactId().setValue(artifactId);
                        mavenProjectXml.getVersion().setValue(version);
                        mavenProjectXml.getName().setValue(pluginName);

                        assert root != null;
                        XmlTag properties = root.findFirstSubTag("properties");
                        assert properties != null;
                        XmlTag buildProperty = properties.findFirstSubTag("project.build.sourceEncoding");

                        if (configuration.hasAuthors()) {
                            XmlTag authorTag = properties.createChildTag("project.author", null, configuration.authors.get(0), false);
                            properties.addAfter(authorTag, buildProperty);
                            pluginAuthor = configuration.authors.get(0);
                        }

                        for (BuildRepository buildRepository : repositories) {
                            Repository repository = mavenProjectXml.getRepositories().addRepository();
                            repository.getId().setValue(buildRepository.getId());
                            repository.getUrl().setValue(buildRepository.getUrl());
                        }

                        for (BuildDependency buildDependency : dependencies) {
                            Dependency dependency = mavenProjectXml.getDependencies().addDependency();
                            dependency.getGroupId().setValue(buildDependency.getGroupId());
                            dependency.getArtifactId().setValue(buildDependency.getArtifactId());
                            dependency.getVersion().setValue(buildDependency.getVersion());
                            dependency.getScope().setValue(buildDependency.getScope());
                        }

                        PsiDirectory rootDirectoryPsi = PsiManager.getInstance(project).findDirectory(rootDirectory);
                        if (rootDirectoryPsi != null) {
                            rootDirectoryPsi.add(finalPomPsi);
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
    public void finishSetup(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
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
    public BuildSystem reImport(@NotNull Project project, @NotNull PlatformType type) {
        rootDirectory = project.getBaseDir();
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
        // TODO: change some things around to support multiple modules
        mavenProjects.stream().filter(this::isApplicable).limit(1).forEach(p -> {
            artifactId = p.getMavenId().getArtifactId();
            groupId = p.getMavenId().getGroupId();
            version = p.getMavenId().getVersion();

            dependencies = p.getDependencies().stream()
                    .map(d -> new BuildDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope()))
                    .collect(Collectors.toList());
            repositories = p.getRemoteRepositories().stream()
                    .map(r -> new BuildRepository(r.getId(), r.getUrl()))
                    .collect(Collectors.toList());

            pomFile = p.getFile();
            if (p.getSources().size() > 0) {
                sourceDirectory = LocalFileSystem.getInstance().findFileByPath(p.getSources().get(0));
            }
            if (p.getResources().size() > 0) {
                resourceDirectory = LocalFileSystem.getInstance().findFileByPath(p.getResources().get(0).getDirectory());
            }
            if (p.getTestSources().size() > 0) {
                testSourcesDirectory = LocalFileSystem.getInstance().findFileByPath(p.getTestSources().get(0));
            }
            if (p.getTestResources().size() > 0) {
                testResourceDirectory = LocalFileSystem.getInstance().findFileByPath(p.getTestResources().get(0).getDirectory());
            }

            // Set author and plugin name, if set
            ApplicationManager.getApplication().runReadAction(() -> {
                PsiFile psiPomFile = PsiManager.getInstance(project).findFile(pomFile);
                if (psiPomFile instanceof XmlFile) {
                    XmlTag rootTag = ((XmlFile) psiPomFile).getRootTag();

                    if (rootTag != null) {
                        XmlTag nameTag = rootTag.findFirstSubTag("name");

                        if (nameTag != null) {
                            pluginName = nameTag.getValue().getText();
                        }

                        XmlTag propertiesTag = rootTag.findFirstSubTag("properties");

                        if (propertiesTag != null) {
                            XmlTag projectAuthorTag = propertiesTag.findFirstSubTag("project.author");

                            if (projectAuthorTag != null) {
                                pluginAuthor = projectAuthorTag.getValue().getText();
                            }
                        }
                    }
                }
            });
        });

        return this;
    }

    private boolean isApplicable(MavenProject project) {
        MavenImporter importer = new BukkitMavenImporter();
        if (importer.isApplicable(project)) {
            return true;
        }
        importer = new SpigotMavenImporter();
        if (importer.isApplicable(project)) {
            return true;
        }
        importer = new PaperMavenImporter();
        if (importer.isApplicable(project)) {
            return true;
        }
        importer = new SpongeMavenImporter();
        if (importer.isApplicable(project)) {
            return true;
        }
        importer = new BungeeCordMavenImporter();
        return importer.isApplicable(project);
    }
}
