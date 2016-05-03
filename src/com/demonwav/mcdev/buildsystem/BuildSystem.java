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

    protected String pluginName;
    @Nullable
    protected String pluginAuthor;

    protected String buildVersion;

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

    public abstract String getArtifactId();
    public abstract void setArtifactId(@NotNull String artifactId);

    public abstract String getGroupId();
    public abstract void setGroupId(@NotNull String groupId);

    public abstract String getVersion();
    public abstract void setVersion(@NotNull String version);

    public abstract List<BuildDependency> getDependencies();
    public abstract void setDependencies(@NotNull List<BuildDependency> dependencies);

    public abstract List<BuildRepository> getRepositories();
    public abstract void setRepositories(@NotNull List<BuildRepository> repositories);

    public abstract VirtualFile getRootDirectory();
    public abstract void setRootDirectory(@NotNull VirtualFile rootDirectory);

    public abstract VirtualFile getSourceDirectory();
    public abstract VirtualFile getTestDirectory();
    public abstract VirtualFile getResourceDirectory();

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

    /**
     * Called when something may have made modifications to the build config file, and this class must read it again in
     * case there are any changes.
     */
    public abstract void reImport();
}
