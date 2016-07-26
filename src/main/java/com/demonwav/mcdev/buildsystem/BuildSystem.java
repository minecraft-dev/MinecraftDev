package com.demonwav.mcdev.buildsystem;

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.util.Util;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Maven and Gradle build systems. The general contract of any class which implements this is any
 * change in a setter in this class will reflect back on the actual files that these classes represent, and in turn
 * represent changes in the project itself.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class BuildSystem {

    private final static Object lock = new Object();

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
     * {@link #finishSetup(Module, ProjectConfiguration, ProgressIndicator)}.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param project The project
     * @param configurations The configuration objects for the project
     * @author DemonWav
     */
    public abstract void create(@NotNull Project project,
                                @NotNull ProjectConfiguration configurations,
                                @NotNull ProgressIndicator indicator);

    /**
     * This is called after {@link #create(Project, ProjectConfiguration, ProgressIndicator)}, and after the module has set
     * itself up. This is when the build system should make whatever calls are necessary to enable the build system's
     * plugin, and setup whatever run configs should be setup for this build system.
     * <p>
     * It is legal for this method to have different default setups for each platform type, so the PlatformType and
     * ProjectConfiguration are provided here as well.
     *
     * @param module the module
     * @param configurations The configuration object for the project
     */
    public abstract void finishSetup(@NotNull Module module,
                                     @NotNull ProjectConfiguration configurations,
                                     @NotNull ProgressIndicator indicator);

    /**
     * This method performs similarly to {@link #create(Project, ProjectConfiguration, ProgressIndicator)} in that it builds
     * this object's model of the project. The difference here is this method reads the project and builds the model
     * from the current project's state. The includes settings the artifactId, groupId, and version, setting the root
     * directory, building the list of dependencies and repositories, settings the source, test, and resource directories,
     * and setting the build version, and whatever else may be added that consists of this project's build system state.
     *
     * @param module The module
     */
    public abstract Promise<? extends BuildSystem> reImport(@NotNull Module module);

    /**
     * Return true when reImport has run.
     *
     * @return True if reImport has been run.
     */
    public abstract boolean isImported();

    /**
     * Return true when reImport has finished.
     *
     * @return True if reImport has finished.
     */
    public abstract boolean isFinishImport();

    @Nullable
    public static BuildSystem getInstance(@NotNull Module module) {
        // generally the other way around, by synchronizing this block and the reImport for GradleBuildSystem and
        // MavenBuildSystem, performance is significantly increased, as it dramatically decreases the number of times
        // the module will be reimported.
        synchronized (lock) {
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
    }

    @Nullable
    public VirtualFile findFile(@NotNull String path, @NotNull SourceType type) {
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

    @Nullable
    @Contract("null, _ -> null")
    private VirtualFile findFile(List<VirtualFile> dirs, @NotNull String path) {
        VirtualFile file;
        if (dirs == null) {
            return null;
        }
        for (VirtualFile dir : dirs) {
            if (dir == null) {
                continue;
            }

            file = dir.findFileByRelativePath(path);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    protected void createDirectories() {
        createDirectories(rootDirectory);
    }

    protected void createDirectories(@NotNull VirtualFile root) {
        Util.runWriteTask(() -> {
            try {
                sourceDirectories = Collections.singletonList(VfsUtil.createDirectories(root.getPath() + "/src/main/java"));
                resourceDirectories = Collections.singletonList(VfsUtil.createDirectories(root.getPath() + "/src/main/resources"));
                testSourcesDirectories = Collections.singletonList(VfsUtil.createDirectories(root.getPath() + "/src/test/java"));
                testResourceDirectories = Collections.singletonList(VfsUtil.createDirectories(root.getPath() + "/src/test/resources"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("artifactId", artifactId)
            .add("groupId", groupId)
            .add("version", version)
            .add("dependencies", dependencies)
            .add("repositories", repositories)
            .add("rootDirectory", rootDirectory)
            .add("sourceDirectories", sourceDirectories)
            .add("resourceDirectories", resourceDirectories)
            .add("testSourcesDirectories", testSourcesDirectories)
            .add("testResourceDirectories", testResourceDirectories)
            .add("pluginName", pluginName)
            .add("buildVersion", buildVersion)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuildSystem that = (BuildSystem) o;
        return Objects.equal(artifactId, that.artifactId) &&
            Objects.equal(groupId, that.groupId) &&
            Objects.equal(version, that.version) &&
            Objects.equal(dependencies, that.dependencies) &&
            Objects.equal(repositories, that.repositories) &&
            Objects.equal(rootDirectory, that.rootDirectory) &&
            Objects.equal(sourceDirectories, that.sourceDirectories) &&
            Objects.equal(resourceDirectories, that.resourceDirectories) &&
            Objects.equal(testSourcesDirectories, that.testSourcesDirectories) &&
            Objects.equal(testResourceDirectories, that.testResourceDirectories) &&
            Objects.equal(pluginName, that.pluginName) &&
            Objects.equal(buildVersion, that.buildVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(artifactId, groupId, version, dependencies, repositories, rootDirectory, sourceDirectories,
            resourceDirectories, testSourcesDirectories, testResourceDirectories, pluginName, buildVersion);
    }
}
