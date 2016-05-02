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
     * the root directory, create a base project consisting of the necessary configuration files, an empty class with
     * empty initialization methods, and a basic run config which will build the project using the current build system.
     * This method may also assume required fields not included in this base class are set, as the project types will be
     * aware of the build system and it's requirements.
     */
    public abstract void create(@NotNull Project project);

    /**
     * Called when something may have made modifications to the build config file, and this class must read it again in
     * case there are any changes.
     */
    public abstract void reImport();
}
