package com.demonwav.mcdev.buildsystem;

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

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
    protected VirtualFile testSourcesDirectory;
    protected VirtualFile testResourceDirectory;

    /**
     * This refers to the plugin name from the perspective of the build system, that being a name field in the build
     * system's configuration. This is not the actual plugin name, which would be stated in the plugin's description
     * file, or the main class file, depending on the project. This field is null if this value is missing.
     */
    @Nullable
    protected String pluginName;
    /**
     * This refers to the plugin author from the perspective of the build system, that being an author field in the build
     * system's configuration. This is not the actual plugin author's name, which would be stated in the plugin's
     * description file, or the main class, depending on the project. This field is null if this value is missing.
     */
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

    public VirtualFile getTestSourcesDirectory() {
        return testSourcesDirectory;
    }

    public void setTestSourcesDirectory(VirtualFile testSourcesDirectory) {
        this.testSourcesDirectory = testSourcesDirectory;
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
     * {@link #finishSetup(Project, PlatformType, ProjectConfiguration)}.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param project The Project object for this project
     * @param type The type of the project
     * @param configuration The configuration object for the project
     */
    public abstract void create(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration);

    /**
     * This is called after {@link #create(Project, PlatformType, ProjectConfiguration)}, and after the project has set
     * itself up. This is when the build system should make whatever calls are necessary to enable the build system's
     * plugin, and setup whatever run configs should be setup for this build system.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param project The Project object for this project
     * @param type The type of the project
     * @param configuration The configuration object for the project
     */
    public abstract void finishSetup(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration);

    /**
     * This method performs similarly to {@link #create(Project, PlatformType, ProjectConfiguration)} in that it builds
     * this object's model of the project. The difference here is this method reads the project and builds the model
     * from the current project's state. The includes settings the artifactId, groupId, and version, setting the root
     * directory, building the list of dependencies and repositories, settings the source, test, and resource directories,
     * and setting the build version, and whatever else may be added that consists of this project's build system state.*
     *
     * @param project The project The Project object for this project
     * @return this object
     */
    public abstract BuildSystem reImport(@NotNull Project project, @NotNull PlatformType type);

    @NotNull
    public static BuildSystem getInstance(@NotNull Project project) {
        VirtualFile pom = project.getBaseDir().findFileByRelativePath("/src/main/resources/plugin.yml");
        if (pom != null) {
            return new MavenBuildSystem();
        } else {
            return new GradleBuildSystem();
        }
    }
}
