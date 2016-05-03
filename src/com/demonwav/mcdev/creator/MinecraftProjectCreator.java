package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.Type;
import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.settings.MinecraftSettings;
import com.demonwav.mcdev.util.MinecraftTemplate;
import com.demonwav.mcdev.settings.BukkitSettings;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class MinecraftProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private Type type = Type.BUKKIT;
    private Project project = null;
    private BuildSystem buildSystem;

    private MinecraftSettings settings = null;

    private VirtualFile sourceDir;
    private VirtualFile resourceDir;
    private VirtualFile testDir;
    private VirtualFile pomFile;

    public void create() {
        buildSystem.setRootDirectory(root);

        buildSystem.setGroupId(groupId);
        buildSystem.setArtifactId(artifactId);
        buildSystem.setVersion(version);

        buildSystem.setPluginAuthor(settings.author);
        buildSystem.setPluginName(settings.pluginName);

        BuildRepository buildRepository = new BuildRepository();
        BuildDependency dependency = new BuildDependency();
        buildSystem.setRepositories(Collections.singletonList(buildRepository));
        buildSystem.setDependencies(Collections.singletonList(dependency));

        switch (type) {
            case BUKKIT:
                buildRepository.setId("spigot-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/");
                dependency.setGroupId("org.bukkit");
                dependency.setArtifactId("bukkit");
                dependency.setVersion("1.9.2-R0.1-SNAPSHOT");
                break;
            case SPIGOT:
                buildRepository.setId("spigot-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/");
                dependency.setGroupId("org.spigotmc");
                dependency.setArtifactId("spigot-api");
                dependency.setVersion("1.9.2-R0.1-SNAPSHOT");
                break;
            case PAPER:
                buildRepository.setId("destroystokyo-repo");
                buildRepository.setUrl("https://repo.destroystokyo.com/content/groups/public/");
                dependency.setGroupId("com.destroystokyo.paper");
                dependency.setArtifactId("paper-api");
                dependency.setVersion("1.9.2-R0.1-SNAPSHOT");
                break;
            case BUNGEECORD:
                buildRepository.setId("bungeecord-repo");
                buildRepository.setUrl("https://oss.sonatype.org/content/repositories/snapshots");
                dependency.setGroupId("net.md-5");
                dependency.setArtifactId("bungeecord-api");
                dependency.setVersion("1.9-SNAPSHOT");
                break;
            case SPONGE:
                buildRepository.setId("sponge");
                buildRepository.setUrl("http://repo.spongepowered.org/maven");
                dependency.setGroupId("org.spongepowered");
                dependency.setArtifactId("spongeapi");
                dependency.setVersion("4.0.3");
            default:
                break;
        }
        dependency.setScope("provided");

        buildSystem.create(project);
        settings.create(project, buildSystem);
        buildSystem.finishSetup(project);
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

    public MinecraftSettings getSettings() {
        return settings;
    }

    public void setSettings(MinecraftSettings settings) {
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

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public void setBuildSystem(BuildSystem buildSystem) {
        this.buildSystem = buildSystem;
    }

    @Override
    public String toString() {
        return "MinecraftProjectCreator{" +
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

        MinecraftProjectCreator that = (MinecraftProjectCreator) o;

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
