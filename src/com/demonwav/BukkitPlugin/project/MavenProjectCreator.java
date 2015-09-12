/*
 * IntelliJ IDEA Bukkit Support Plugin
 *
 * Written by Kyle Wood (DemonWav)
 * http://demonwav.com
 *
 * MIT License
 */

package com.demonwav.BukkitPlugin.project;

import com.demonwav.BukkitPlugin.BukkitProject;
import com.demonwav.BukkitPlugin.util.BukkitTemplate;
import com.demonwav.BukkitPlugin.util.MavenSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Objects;

public class MavenProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private BukkitProject.Type type = BukkitProject.Type.BUKKIT;
    private Project project = null;

    private BukkitProjectSettingsWizardStep.S settings = null;

    private VirtualFile sourceDir;
    private VirtualFile resourceDir;
    private VirtualFile testDir;
    private VirtualFile pomFile;

    public void create() {
        root.refresh(false, true);
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                // It seems there's no virtual file way of making the whole path in one go
                // idk, maybe I'm stupid, but I couldn't find it
                sourceDir = root.createChildDirectory(this, "src")
                        .createChildDirectory(this, "main")
                        .createChildDirectory(this, "java");
                VirtualFile file = sourceDir;
                String[] files = groupId.split("\\.");
                for (String s : files)
                    file = file.createChildDirectory(this, s);
                file = file.createChildDirectory(this, artifactId);

                resourceDir = root.findOrCreateChildData(this, "src")
                        .findOrCreateChildData(this, "main")
                        .createChildDirectory(this, "resource");

                testDir = root.findOrCreateChildData(this, "src")
                        .createChildDirectory(this, "test")
                        .createChildDirectory(this, "java");

                pomFile = root.createChildData(project, "pom.xml");

                MavenSettings mavenSettings = new MavenSettings();
                mavenSettings.setGroupId(groupId);
                mavenSettings.setArtifactId(artifactId);
                mavenSettings.setVersion(version);
                if (settings.author != null && !settings.author.trim().isEmpty())
                    mavenSettings.setAuthor(settings.author);

                switch (type) {
                    case BUKKIT:
                        mavenSettings.setRepoId("spigot-repo");
                        mavenSettings.setRepoUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/");
                        mavenSettings.setApiName("Bukkit");
                        mavenSettings.setApiGroupId("org.bukkit");
                        mavenSettings.setApiArtifactId("bukkit");
                        mavenSettings.setApiVersion("1.8.8-R0.1-SNAPSHOT");
                        break;
                    case SPIGOT:
                        mavenSettings.setRepoId("spigot-repo");
                        mavenSettings.setRepoUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/");
                        mavenSettings.setApiName("Spigot");
                        mavenSettings.setApiGroupId("org.spigotmc");
                        mavenSettings.setApiArtifactId("spigot-api");
                        mavenSettings.setApiVersion("1.8.8-R0.1-SNAPSHOT");
                        break;
                    case BUNGEECORD:
                        mavenSettings.setRepoId("bungeecord-repo");
                        mavenSettings.setRepoUrl("https://oss.sonatype.org/content/repositories/snapshots");
                        mavenSettings.setApiName("BungeeCord");
                        mavenSettings.setApiGroupId("net.md-5");
                        mavenSettings.setApiArtifactId("bungeecord-api");
                        mavenSettings.setApiVersion("1.8-SNAPSHOT");
                        break;
                    default:
                        break;
                }
                BukkitTemplate.applyPomTemplate(project, pomFile, mavenSettings);
                VirtualFile mainClass = file.findOrCreateChildData(this, settings.mainClass + ".java");
                String packageName = groupId + "." + artifactId;
                BukkitTemplate.applyMainClassTemplate(project, mainClass, packageName, settings.mainClass);
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

    public BukkitProject.Type getType() {
        return type;
    }

    public void setType(BukkitProject.Type type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public BukkitProjectSettingsWizardStep.S getSettings() {
        return settings;
    }

    public void setSettings(BukkitProjectSettingsWizardStep.S settings) {
        this.settings = settings;
    }

    // Because IntelliJ will generate these
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
