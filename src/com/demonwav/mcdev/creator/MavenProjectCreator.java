/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.util.BukkitTemplate;
import com.demonwav.mcdev.util.ProjectSettings;
import com.demonwav.mcdev.util.MavenSettings;

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

public class MavenProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private Type type = Type.BUKKIT;
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

    public VirtualFile getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(VirtualFile sourceDir) {
        this.sourceDir = sourceDir;
    }

    public VirtualFile getResourceDir() {
        return resourceDir;
    }

    public void setResourceDir(VirtualFile resourceDir) {
        this.resourceDir = resourceDir;
    }

    public VirtualFile getTestDir() {
        return testDir;
    }

    public void setTestDir(VirtualFile testDir) {
        this.testDir = testDir;
    }

    public VirtualFile getPomFile() {
        return pomFile;
    }

    public void setPomFile(VirtualFile pomFile) {
        this.pomFile = pomFile;
    }

    @Override
    public String toString() {
        return "MavenProjectCreator{" +
                "root=" + root +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", type=" + type +
                ", project=" + project +
                ", settings=" + settings +
                ", sourceDir=" + sourceDir +
                ", resourceDir=" + resourceDir +
                ", testDir=" + testDir +
                ", pomFile=" + pomFile +
                '}';
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MavenProjectCreator that = (MavenProjectCreator) o;

        if (root != null ? !root.equals(that.root) : that.root != null) {
            return false;
        }
        if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) {
            return false;
        }
        if (artifactId != null ? !artifactId.equals(that.artifactId) : that.artifactId != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        if (type != that.type) return false;
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (settings != null ? !settings.equals(that.settings) : that.settings != null) {
            return false;
        }
        if (sourceDir != null ? !sourceDir.equals(that.sourceDir) : that.sourceDir != null) {
            return false;
        }
        if (resourceDir != null ? !resourceDir.equals(that.resourceDir) : that.resourceDir != null) {
            return false;
        }
        if (testDir != null ? !testDir.equals(that.testDir) : that.testDir != null) {
            return false;
        }
        return pomFile != null ? pomFile.equals(that.pomFile) : that.pomFile == null;

    }

    @Override
    public int hashCode() {
        int result = root != null ? root.hashCode() : 0;
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (settings != null ? settings.hashCode() : 0);
        result = 31 * result + (sourceDir != null ? sourceDir.hashCode() : 0);
        result = 31 * result + (resourceDir != null ? resourceDir.hashCode() : 0);
        result = 31 * result + (testDir != null ? testDir.hashCode() : 0);
        result = 31 * result + (pomFile != null ? pomFile.hashCode() : 0);
        return result;
    }
}
