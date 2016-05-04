package com.demonwav.mcdev.buildsystem;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for Maven and Gradle build systems. The general contract of any class which implements this is any
 * change in a setter in this class will reflect back on the actual files that these classes represent, and in turn
 * represent changes in the project itself.
 */
public abstract class BuildSystem {

    protected String artifactId;
    protected String groupId;
    protected String version;

    protected List<BuildDependency> dependencies;
    protected List<BuildRepository> repositories;
    protected VirtualFile rootDirectory;

    protected VirtualFile sourceDirectory;
    protected VirtualFile resourceDirectory;
    protected VirtualFile testDirectory;

    protected String pluginName;
    @Nullable
    protected String pluginAuthor;

    protected String buildVersion;

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<BuildDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<BuildDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<BuildRepository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<BuildRepository> repositories) {
        this.repositories = repositories;
    }

    public VirtualFile getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(VirtualFile rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public VirtualFile getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(VirtualFile sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public VirtualFile getResourceDirectory() {
        return resourceDirectory;
    }

    public void setResourceDirectory(VirtualFile resourceDirectory) {
        this.resourceDirectory = resourceDirectory;
    }

    public VirtualFile getTestDirectory() {
        return testDirectory;
    }

    public void setTestDirectory(VirtualFile testDirectory) {
        this.testDirectory = testDirectory;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(@NotNull String pluginName) {
        this.pluginName = pluginName;
    }

    @Nullable
    public String getPluginAuthor() {
        return pluginAuthor;
    }

    public void setPluginAuthor(@Nullable String pluginAuthor) {
        this.pluginAuthor = pluginAuthor;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(@NotNull String buildVersion) {
        this.buildVersion = buildVersion;
    }

    /**
     * Assuming the artifact ID, group ID, and  version are set, along with whatever dependencies and repositories and
     * the root directory, create a base project consisting of the necessary build system configuration files and
     * directory structure. This method does not create any classes or project-specific things, nor does it set up
     * any build configurations or enable the plugin for this build config. This will be done in
     * {@link #finishSetup(Project)}.
     *
     * @param project The Project object for this project
     */
    public abstract void create(@NotNull Project project);

    /**
     * This is called after {@link #create(Project)}, and after the project has set itself up. This is when the build
     * system should make whatever calls are necessary to enable the build system's plugin, and setup whatever run
     * configs should be setup for this build system.
     *
     * @param project The Project object for this project
     */
    public abstract void finishSetup(@NotNull Project project);
}
