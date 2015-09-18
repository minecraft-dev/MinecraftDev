/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.bukkitplugin.creator;

import com.demonwav.bukkitplugin.BukkitProject;
import com.demonwav.bukkitplugin.BukkitProject.Type;
import com.demonwav.bukkitplugin.util.ProjectSettings;
import com.demonwav.bukkitplugin.util.BukkitTemplate;
import com.demonwav.bukkitplugin.util.MavenSettings;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class MavenProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private BukkitProject.Type type = BukkitProject.Type.BUKKIT;
    private Project project = null;

    private ProjectSettings settings = null;

    private VirtualFile sourceDir;
    private VirtualFile resourceDir;
    private VirtualFile testDir;
    private VirtualFile pomFile;

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
                for (String s : files)
                    file = file.createChildDirectory(this, s);

                pomFile = root.createChildData(project, "pom.xml");

                MavenSettings mavenSettings = new MavenSettings();
                mavenSettings.groupId = groupId;
                mavenSettings.artifactId = artifactId;
                mavenSettings.version = version;
                if (settings.author != null && !settings.author.trim().isEmpty())
                    mavenSettings.author = settings.author;

                switch (type) {
                    case BUKKIT:
                        mavenSettings.repoId = "spigot-repo";
                        mavenSettings.repoUrl = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/";
                        mavenSettings.apiName = "Bukkit";
                        mavenSettings.apiGroupId = "org.bukkit";
                        mavenSettings.apiArtifactId = "bukkit";
                        mavenSettings.apiVersion = "1.8.8-R0.1-SNAPSHOT";
                        break;
                    case SPIGOT:
                        mavenSettings.repoId = "spigot-repo";
                        mavenSettings.repoUrl = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/";
                        mavenSettings.apiName = "Spigot";
                        mavenSettings.apiGroupId = "org.spigotmc";
                        mavenSettings.apiArtifactId = "spigot-api";
                        mavenSettings.apiVersion = "1.8.8-R0.1-SNAPSHOT";
                        break;
                    case BUNGEECORD:
                        mavenSettings.repoId = "bungeecord-repo";
                        mavenSettings.repoUrl = "https://oss.sonatype.org/content/repositories/snapshots";
                        mavenSettings.apiName = "BungeeCord";
                        mavenSettings.apiGroupId = "net.md-5";
                        mavenSettings.apiArtifactId = "bungeecord-api";
                        mavenSettings.apiVersion = "1.8-SNAPSHOT";
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
                if (mainClassPsi != null)
                    EditorHelper.openInEditor(mainClassPsi);

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

    public VirtualFile getRoot() {
        return root;
    }

    public void setRoot(VirtualFile root) {
        this.root = root;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectSettings getSettings() {
        return settings;
    }

    public void setSettings(ProjectSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MavenProjectCreator)) return false;

        MavenProjectCreator that = (MavenProjectCreator) o;

        return Objects.equals(getRoot(), that.getRoot()) &&
            Objects.equals(getGroupId(), that.getGroupId()) &&
            Objects.equals(getArtifactId(), that.getArtifactId()) &&
            Objects.equals(getVersion(), that.getVersion()) &&
            getType() == that.getType();
    }

    @Override
    public int hashCode() {
        int result = getRoot() != null ? getRoot().hashCode() : 0;
        result = 31 * result + (getGroupId() != null ? getGroupId().hashCode() : 0);
        result = 31 * result + (getArtifactId() != null ? getArtifactId().hashCode() : 0);
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        result = 31 * result + (getType() != null ? getType().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MavenProjectCreator{" +
            "root=" + root +
            ", groupId='" + groupId + '\'' +
            ", artifactId='" + artifactId + '\'' +
            ", version='" + version + '\'' +
            ", type=" + type +
            '}';
    }
}
