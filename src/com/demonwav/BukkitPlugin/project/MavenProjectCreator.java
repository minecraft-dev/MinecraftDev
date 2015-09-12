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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
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

    public void create() {
        new File(root.getCanonicalPath(), "src/main/java/" + groupId.replaceAll("\\.", "/")).mkdirs();
        new File(root.getCanonicalPath(), "src/main/resources/").mkdirs();
        new File(root.getCanonicalPath(), "src/test/java/").mkdirs();

        try {
            root.createChildData(project, "pom.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
