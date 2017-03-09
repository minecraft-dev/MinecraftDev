/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

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
import com.demonwav.mcdev.platform.canary.CanaryTemplate;
import com.demonwav.mcdev.platform.sponge.SpongeTemplate;
import com.demonwav.mcdev.util.Util;
import com.google.common.base.Strings;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomManager;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

public class MavenBuildSystem extends BuildSystem {

    private VirtualFile pomFile;

    @Override
    public void create(@NotNull Project project, @NotNull ProjectConfiguration configuration, @NotNull ProgressIndicator indicator) {
        getRootDirectory().refresh(false, true);

        Util.runWriteTask(() -> {
            createDirectories();

            PsiFile pomPsi = null;

            // TODO: Generify the pom, having multiple projects doesn't allow a different pom for each project
            String text = null;

            if (configuration.type == PlatformType.BUKKIT || configuration.type == PlatformType.SPIGOT ||
                    configuration.type == PlatformType.PAPER) {
                text = BukkitTemplate.applyPomTemplate(project, getBuildVersion());
            } else if (configuration.type == PlatformType.BUNGEECORD) {
                text = BungeeCordTemplate.applyPomTemplate(project, getBuildVersion());
            } else if (configuration.type == PlatformType.SPONGE) {
                text = SpongeTemplate.applyPomTemplate(project, getBuildVersion());
            } else if (configuration.type == PlatformType.CANARY || configuration.type == PlatformType.NEPTUNE) {
                text = CanaryTemplate.applyPomTemplate(project, getBuildVersion());
            }

            if (text != null) {
                pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, text);
            }

            if (pomPsi != null) {
                pomPsi.setName("pom.xml");

                final XmlFile pomXmlPsi = (XmlFile) pomPsi;
                final PsiFile finalPomPsi = pomPsi;
                new WriteCommandAction.Simple(project, pomPsi) {
                    @Override
                    protected void run() throws Throwable {
                        XmlTag root = pomXmlPsi.getRootTag();

                        DomManager manager = DomManager.getDomManager(project);
                        MavenProjectXml mavenProjectXml = manager.getFileElement(pomXmlPsi, MavenProjectXml.class, "project")
                            .getRootElement();

                        mavenProjectXml.getGroupId().setValue(getGroupId());
                        mavenProjectXml.getArtifactId().setValue(getArtifactId());
                        mavenProjectXml.getVersion().setValue(getVersion());
                        mavenProjectXml.getName().setValue(getPluginName());

                        if (root == null) {
                            return;
                        }
                        XmlTag properties = root.findFirstSubTag("properties");
                        if (properties == null) {
                            return;
                        }

                        if (!Strings.isNullOrEmpty(configuration.website)) {
                            XmlTag url = root.createChildTag("url", null, configuration.website, false);
                            root.addAfter(url, properties);
                        }

                        if (!Strings.isNullOrEmpty(configuration.description)) {
                            XmlTag description = root.createChildTag("description", null, configuration.description, false);
                            root.addBefore(description, properties);
                        }

                        for (BuildRepository buildRepository : getRepositories()) {
                            Repository repository = mavenProjectXml.getRepositories().addRepository();
                            repository.getId().setValue(buildRepository.getId());
                            repository.getUrl().setValue(buildRepository.getUrl());
                        }

                        for (BuildDependency buildDependency : getDependencies()) {
                            Dependency dependency = mavenProjectXml.getDependencies().addDependency();
                            dependency.getGroupId().setValue(buildDependency.getGroupId());
                            dependency.getArtifactId().setValue(buildDependency.getArtifactId());
                            dependency.getVersion().setValue(buildDependency.getVersion());
                            dependency.getScope().setValue(buildDependency.getScope());
                        }

                        PsiDirectory rootDirectoryPsi = PsiManager.getInstance(project).findDirectory(getRootDirectory());
                        if (rootDirectoryPsi != null) {
                            rootDirectoryPsi.add(finalPomPsi);
                        }

                        pomFile = getRootDirectory().findChild("pom.xml");
                        // Reformat the code to match their code style
                        PsiFile pomFilePsi = PsiManager.getInstance(project).findFile(pomFile);
                        if (pomFilePsi != null) {
                            new ReformatCodeProcessor(pomFilePsi, false).run();
                        }
                    }
                }.execute();
            }
        });
    }

    @Override
    public void finishSetup(@NotNull Module module, @NotNull Collection<? extends ProjectConfiguration> configurations, @NotNull ProgressIndicator indicator) {
        Util.runWriteTask(() -> {
            Project project = module.getProject();

            // Force Maven to setup the project
            MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
            manager.addManagedFilesOrUnignore(Collections.singletonList(pomFile));
            manager.getImportingSettings().setDownloadDocsAutomatically(true);
            manager.getImportingSettings().setDownloadSourcesAutomatically(true);

            // Setup the default Maven run config
            if (getRootDirectory().getCanonicalPath() != null) {
                MavenRunnerParameters params = new MavenRunnerParameters();
                params.setWorkingDirPath(getRootDirectory().getCanonicalPath());
                params.setGoals(Arrays.asList("clean", "package"));
                RunnerAndConfigurationSettings runnerSettings = MavenRunConfigurationType
                    .createRunnerAndConfigurationSettings(null, null, params, module.getProject());
                runnerSettings.setName(module.getName() + " build");
                RunManager.getInstance(project).addConfiguration(runnerSettings, false);
            }
        });
    }
}
