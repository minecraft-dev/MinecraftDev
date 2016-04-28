/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.creator;

import com.demonwav.util.BukkitTemplate;
import com.demonwav.util.MavenSettings;
import com.demonwav.util.ProjectSettings;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.util.Arrays;

@ToString
@EqualsAndHashCode
public class MavenProjectCreator {

    @Getter @Setter private VirtualFile root = null;
    @Getter @Setter private String groupId = null;
    @Getter @Setter private String artifactId = null;
    @Getter @Setter private String version = null;
    @Getter @Setter private Type type = Type.BUKKIT;
    @Getter @Setter private Project project = null;

    @Getter @Setter private ProjectSettings settings = null;

    @Getter @Setter private VirtualFile sourceDir;
    @Getter @Setter private VirtualFile resourceDir;
    @Getter @Setter private VirtualFile testDir;
    @Getter @Setter private VirtualFile pomFile;

    public void create() {
        root.refresh(false, true);
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                sourceDir = VfsUtil.createDirectories(root.getPath() + "/src/main/java");
                resourceDir = VfsUtil.createDirectories(root.getPath() + "/src/main/resources");
                testDir = VfsUtil.createDirectories(root.getPath() + "/src/test/java");

                // Create plugin main class
                VirtualFile file = sourceDir;
                String[] files = groupId.split("\\.");
                for (String s : files) {
                    file = file.createChildDirectory(this, s);
                }

                pomFile = root.createChildData(project, "pom.xml");

                MavenSettings mavenSettings = new MavenSettings();
                mavenSettings.groupId = groupId;
                mavenSettings.artifactId = artifactId;
                mavenSettings.version = version;

                if (settings.author != null && !settings.author.trim().isEmpty()) {
                    mavenSettings.author = settings.author;
                }

                switch (type) {
                    case BUKKIT:
                        mavenSettings.repoId = "spigot-repo";
                        mavenSettings.repoUrl = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/";
                        mavenSettings.apiName = "Bukkit";
                        mavenSettings.apiGroupId = "org.bukkit";
                        mavenSettings.apiArtifactId = "bukkit";
                        mavenSettings.apiVersion = "1.9.2-R0.1-SNAPSHOT";
                        break;
                    case SPIGOT:
                        mavenSettings.repoId = "spigot-repo";
                        mavenSettings.repoUrl = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/";
                        mavenSettings.apiName = "Spigot";
                        mavenSettings.apiGroupId = "org.spigotmc";
                        mavenSettings.apiArtifactId = "spigot-api";
                        mavenSettings.apiVersion = "1.9.2-R0.1-SNAPSHOT";
                        break;
                    case BUNGEECORD:
                        mavenSettings.repoId = "bungeecord-repo";
                        mavenSettings.repoUrl = "https://oss.sonatype.org/content/repositories/snapshots";
                        mavenSettings.apiName = "BungeeCord";
                        mavenSettings.apiGroupId = "net.md-5";
                        mavenSettings.apiArtifactId = "bungeecord-api";
                        mavenSettings.apiVersion = "1.9-SNAPSHOT";
                        break;
                    default:
                        break;
                }
                // Create the pom.xml, main class, and plugin.yml
                BukkitTemplate.applyPomTemplate(project, pomFile, mavenSettings);
                VirtualFile mainClass = file.findOrCreateChildData(this, settings.mainClass + ".java");
                BukkitTemplate.applyMainClassTemplate(project, mainClass, groupId, settings.mainClass, type != Type.BUNGEECORD);
                VirtualFile pluginYml = resourceDir.findOrCreateChildData(this, "plugin.yml");
                BukkitTemplate.applyPluginYmlTemplate(project, pluginYml, type, settings, groupId);

                // Set the editor focus on the main class
                PsiFile mainClassPsi = PsiManager.getInstance(project).findFile(mainClass);
                if (mainClassPsi != null) {
                    EditorHelper.openInEditor(mainClassPsi);
                }

                // Force Maven to setup the project
                MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles();

                // Setup the default Maven run config
                if (root.getCanonicalPath() != null) {
                    MavenRunnerParameters params = new MavenRunnerParameters();
                    params.setWorkingDirPath(root.getCanonicalPath());
                    params.setGoals(Arrays.asList("clean", "package"));
                    RunnerAndConfigurationSettings runnerSettings = MavenRunConfigurationType.createRunnerAndConfigurationSettings(null, null, params, project);
                    runnerSettings.setName("clean package");
                    RunManager.getInstance(project).addConfiguration(runnerSettings, true);
                    RunManager.getInstance(project).setSelectedConfiguration(runnerSettings);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
