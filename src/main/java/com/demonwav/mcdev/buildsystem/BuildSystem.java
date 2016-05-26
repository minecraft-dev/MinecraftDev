package com.demonwav.mcdev.buildsystem;

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Maven and Gradle build systems. The general contract of any class which implements this is any
 * change in a setter in this class will reflect back on the actual files that these classes represent, and in turn
 * represent changes in the project itself.
 */
public abstract class BuildSystem {

    private static final Map<Module, BuildSystem> map = new HashMap<>();

    protected String artifactId;
    protected String groupId;
    protected String version;

    protected List<BuildDependency> dependencies;
    protected List<BuildRepository> repositories;
    protected VirtualFile rootDirectory;

    protected List<VirtualFile> sourceDirectories;
    protected List<VirtualFile> resourceDirectories;
    protected List<VirtualFile> testSourcesDirectories;
    protected List<VirtualFile> testResourceDirectories;

    /**
     * This refers to the plugin name from the perspective of the build system, that being a name field in the build
     * system's configuration. This is not the actual plugin name, which would be stated in the plugin's description
     * file, or the main class file, depending on the project. This field is null if this value is missing.
     */
    @Nullable
    protected String pluginName;

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

    public List<VirtualFile> getSourceDirectories() {
        return sourceDirectories;
    }

    public void setSourceDirectories(List<VirtualFile> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    public List<VirtualFile> getResourceDirectories() {
        return resourceDirectories;
    }

    public void setResourceDirectories(List<VirtualFile> resourceDirectories) {
        this.resourceDirectories = resourceDirectories;
    }

    public List<VirtualFile> getTestSourcesDirectories() {
        return testSourcesDirectories;
    }

    public void setTestSourcesDirectories(List<VirtualFile> testSourcesDirectories) {
        this.testSourcesDirectories = testSourcesDirectories;
    }

    @Nullable
    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(@NotNull String pluginName) {
        this.pluginName = pluginName;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public void setBuildVersion(@NotNull String buildVersion) {
        this.buildVersion = buildVersion;
    }

    /**
     * Assuming the artifact ID, group ID, and  version are set, along with whatever dependencies and repositories and
     * the root directory, create a base module consisting of the necessary build system configuration files and
     * directory structure. This method does not create any classes or project-specific things, nor does it set up
     * any build configurations or enable the plugin for this build config. This will be done in
     * {@link #finishSetup(Module, PlatformType, ProjectConfiguration, ProgressIndicator)}.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param module The module
     * @param type The type of the project
     * @param configuration The configuration object for the project
     */
    public abstract void create(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration, @NotNull ProgressIndicator indicator);

    /**
     * This is called after {@link #create(Module, PlatformType, ProjectConfiguration, ProgressIndicator)}, and after the module has set
     * itself up. This is when the build system should make whatever calls are necessary to enable the build system's
     * plugin, and setup whatever run configs should be setup for this build system.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param module the module
     * @param type The type of the project
     * @param configuration The configuration object for the project
     */
    public abstract void finishSetup(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration, @NotNull ProgressIndicator indicator);

    /**
     * This method performs similarly to {@link #create(Module, PlatformType, ProjectConfiguration, ProgressIndicator)} in that it builds
     * this object's model of the project. The difference here is this method reads the project and builds the model
     * from the current project's state. The includes settings the artifactId, groupId, and version, setting the root
     * directory, building the list of dependencies and repositories, settings the source, test, and resource directories,
     * and setting the build version, and whatever else may be added that consists of this project's build system state.*
     *
     * @param module The module
     */
    public abstract void reImport(@NotNull Module module, @NotNull PlatformType type);

    @Nullable
    public static BuildSystem getInstance(@NotNull Module module) {
        return map.computeIfAbsent(module, (m -> {
            VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
            if (roots.length > 0) {
                VirtualFile root = roots[0];
                if (root != null) {
                    VirtualFile pom = root.findChild("pom.xml");
                    VirtualFile gradle = root.findChild("build.gradle");

                    if (pom != null) {
                        return new MavenBuildSystem();
                    } else if (gradle != null) {
                        return new GradleBuildSystem();
                    } else {
                        // We need to check if this is a multi-module gradle project
                        Project project = module.getProject();
                        String[] paths = ModuleManager.getInstance(project).getModuleGroupPath(module);
                        if (paths != null && paths.length > 0) {
                            // The first element is the parent
                            String parentName = paths[0];
                            Module parentModule = ModuleManager.getInstance(project).findModuleByName(parentName);

                            if (parentModule != null) {
                                root = ModuleRootManager.getInstance(parentModule).getContentRoots()[0];
                                pom = root.findChild("pom.xml");
                                gradle = root.findChild("build.gradle");

                                if (pom != null) {
                                    return new MavenBuildSystem();
                                } else if (gradle != null) {
                                    return new GradleBuildSystem();
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }));
    }

    @Nullable
    public VirtualFile findFile(String path, SourceType type) {
        switch (type) {
            case SOURCE:
                return findFile(sourceDirectories, path);
            case RESOURCE:
                return findFile(resourceDirectories, path);
            case TEST_SOURCE:
                return findFile(testSourcesDirectories, path);
            case TEST_RESOURCE:
                return findFile(testResourceDirectories, path);
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "BuildSystem{" +
                "artifactId='" + artifactId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", version='" + version + '\'' +
                ", dependencies=" + dependencies +
                ", repositories=" + repositories +
                ", rootDirectory=" + rootDirectory +
                ", sourceDirectories=" + sourceDirectories +
                ", resourceDirectories=" + resourceDirectories +
                ", testSourcesDirectories=" + testSourcesDirectories +
                ", testResourceDirectories=" + testResourceDirectories +
                ", pluginName='" + pluginName + '\'' +
                ", buildVersion='" + buildVersion + '\'' +
                '}';
    }

    private VirtualFile findFile(List<VirtualFile> dirs, String path) {
        VirtualFile file;
        if (dirs == null) {
            return null;
        }
        for (VirtualFile dir : dirs) {
            file = dir.findFileByRelativePath(path);
            if (file != null) {
                return file;
            }
        }
        return null;
    }
}
