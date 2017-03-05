/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator;

import static com.demonwav.mcdev.platform.PlatformType.FORGE;
import static com.demonwav.mcdev.platform.PlatformType.SPONGE;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;
import com.demonwav.mcdev.platform.canary.CanaryProjectConfiguration;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class MinecraftProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private Module module = null;
    private BuildSystem buildSystem;

    private final Map<PlatformType, ProjectConfiguration> settings = Maps.newLinkedHashMap();

    private VirtualFile sourceDir;
    private VirtualFile resourceDir;
    private VirtualFile testDir;
    private VirtualFile pomFile;

    public void create() {
        buildSystem.setRootDirectory(root);

        buildSystem.setGroupId(groupId);
        buildSystem.setArtifactId(artifactId);
        buildSystem.setVersion(version);

        buildSystem.setPluginName(settings.values().iterator().next().pluginName);

        Set<BuildRepository> buildRepositories = buildSystem.getRepositories();
        Set<BuildDependency> dependencies = buildSystem.getDependencies();

        if (settings.size() == 1) {
            doSingleModuleCreate();
        } else {
            doMultiModuleCreate();
        }
    }

    private void doSingleModuleCreate() {
        ProjectConfiguration configuration = settings.values().iterator().next();
        addDependencies(configuration, buildSystem);

        ProgressManager.getInstance().run(new Task.Backgroundable(module.getProject(), "Setting Up Project", false) {
            @Override
            public boolean shouldStartInBackground() {
                return false;
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                buildSystem.create(module.getProject(), configuration, indicator);
                configuration.create(module.getProject(), buildSystem, indicator);
                configuration.type.getType().performCreationSettingSetup(module.getProject());
                buildSystem.finishSetup(module, ImmutableList.of(configuration), indicator);
            }
        });
    }

    private void doMultiModuleCreate() {
        if (!(buildSystem instanceof GradleBuildSystem)) {
            throw new IllegalStateException("BuildSystem must be Gradle");
        }

        GradleBuildSystem gradleBuildSystem = (GradleBuildSystem) buildSystem;
        ProgressManager.getInstance().run(new Task.Backgroundable(module.getProject(), "Setting Up Project", false) {
            @Override
            public boolean shouldStartInBackground() {
                return false;
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                Map<GradleBuildSystem, ProjectConfiguration> map = gradleBuildSystem
                    .createMultiModuleProject(module.getProject(), settings, indicator);

                map.forEach((g, p) -> {
                    p.create(module.getProject(), g, indicator);
                    p.type.getType().performCreationSettingSetup(module.getProject());
                });
                gradleBuildSystem.finishSetup(module, map.values(), indicator);
            }
        });
    }

    public static void addDependencies(@NotNull ProjectConfiguration configuration,
                                       @NotNull BuildSystem buildSystem) {
        // Forge doesn't have a dependency like this
        if (configuration.type == FORGE) {
            return;
        }

        BuildRepository buildRepository = new BuildRepository();
        BuildDependency buildDependency = new BuildDependency();

        // Sponge projects using Gradle use SpongeGradle which automatically adds the required repositories
        if (configuration.type != SPONGE || !(buildSystem instanceof GradleBuildSystem)) {
            buildSystem.getRepositories().add(buildRepository);
        }

        buildSystem.getDependencies().add(buildDependency);
        switch (configuration.type) {
            case BUKKIT:
                buildRepository.setId("spigotmc-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/groups/public/");
                buildDependency.setGroupId("org.bukkit");
                buildDependency.setArtifactId("bukkit");
                buildDependency.setVersion(((BukkitProjectConfiguration) configuration).minecraftVersion + "-R0.1-SNAPSHOT");
                break;
            case SPIGOT:
                buildRepository.setId("spigotmc-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/groups/public/");
                buildDependency.setGroupId("org.spigotmc");
                buildDependency.setArtifactId("spigot-api");
                buildDependency.setVersion(((BukkitProjectConfiguration) configuration).minecraftVersion + "-R0.1-SNAPSHOT");
                addSonatype(buildSystem.getRepositories());
                break;
            case PAPER:
                buildRepository.setId("destroystokyo-repo");
                buildRepository.setUrl("https://repo.destroystokyo.com/repository/maven-public/");
                buildDependency.setGroupId("com.destroystokyo.paper");
                buildDependency.setArtifactId("paper-api");
                buildDependency.setVersion(((BukkitProjectConfiguration) configuration).minecraftVersion + "-R0.1-SNAPSHOT");
                addSonatype(buildSystem.getRepositories());
                break;
            case BUNGEECORD:
                buildRepository.setId("sonatype-oss-repo");
                buildRepository.setUrl("https://oss.sonatype.org/content/groups/public/");
                buildDependency.setGroupId("net.md-5");
                buildDependency.setArtifactId("bungeecord-api");
                buildDependency.setVersion(((BungeeCordProjectConfiguration) configuration).minecraftVersion + "-SNAPSHOT");
                break;
            case SPONGE:
                buildRepository.setId("spongepowered-repo");
                buildRepository.setUrl("http://repo.spongepowered.org/maven/");
                buildDependency.setGroupId("org.spongepowered");
                buildDependency.setArtifactId("spongeapi");
                if (configuration instanceof SpongeProjectConfiguration) {
                    buildDependency.setVersion(((SpongeProjectConfiguration) configuration).spongeApiVersion);
                } else {
                    buildDependency.setVersion(((SpongeForgeProjectConfiguration) configuration).spongeApiVersion);
                }
                break;
            case CANARY:
                if (!((CanaryProjectConfiguration) configuration).canaryVersion.endsWith("-SNAPSHOT")) {
                    buildRepository.setId("vi-releases");
                    buildRepository.setUrl("http://repo.visualillusionsent.net:8888/repository/internal/");
                } else {
                    buildRepository.setId("vi-snapshots");
                    buildRepository.setUrl("http://repo.visualillusionsent.net:8888/repository/snapshots/");
                }
                buildDependency.setGroupId("net.canarymod");
                buildDependency.setArtifactId("CanaryLib");
                buildDependency.setVersion(((CanaryProjectConfiguration) configuration).canaryVersion);
                break;
            case NEPTUNE:
                if (!((CanaryProjectConfiguration) configuration).canaryVersion.endsWith("-SNAPSHOT")) {
                    buildRepository.setId("lex-releases");
                    buildRepository.setUrl("http://repo.lexteam.xyz/maven/releases/");
                } else {
                    buildRepository.setId("lex-snapshots");
                    buildRepository.setUrl("http://repo.lexteam.xyz/maven/snapshots/");
                }
                addVIRepo(buildSystem.getRepositories());
                buildDependency.setGroupId("org.neptunepowered");
                buildDependency.setArtifactId("NeptuneLib");
                buildDependency.setVersion(((CanaryProjectConfiguration) configuration).canaryVersion);
                break;
            default:
        }
        buildDependency.setScope("provided");
    }

    private static void addSonatype(Set<BuildRepository> buildRepositories) {
        buildRepositories.add(new BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"));
    }

    private static void addVIRepo(Set<BuildRepository> buildRepositories) {
        buildRepositories.add(new BuildRepository("vi-releases", "http://repo.visualillusionsent.net:8888/repository/internal/"));
        buildRepositories.add(new BuildRepository("vi-snapshots", "http://repo.visualillusionsent.net:8888/repository/snapshots/"));
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

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public Map<PlatformType, ProjectConfiguration> getSettings() {
        return settings;
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
        return MoreObjects.toStringHelper(this)
                          .add("root", root)
                          .add("groupId", groupId)
                          .add("artifactId", artifactId)
                          .add("version", version)
                          .add("module", module)
                          .add("buildSystem", buildSystem)
                          .add("settings", settings)
                          .add("sourceDir", sourceDir)
                          .add("resourceDir", resourceDir)
                          .add("testDir", testDir)
                          .add("pomFile", pomFile)
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
        MinecraftProjectCreator that = (MinecraftProjectCreator) o;
        return Objects.equal(root, that.root) &&
            Objects.equal(groupId, that.groupId) &&
            Objects.equal(artifactId, that.artifactId) &&
            Objects.equal(version, that.version) &&
            Objects.equal(module, that.module) &&
            Objects.equal(buildSystem, that.buildSystem) &&
            Objects.equal(settings, that.settings) &&
            Objects.equal(sourceDir, that.sourceDir) &&
            Objects.equal(resourceDir, that.resourceDir) &&
            Objects.equal(testDir, that.testDir) &&
            Objects.equal(pomFile, that.pomFile);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root, groupId, artifactId, version, module,
            buildSystem, settings, sourceDir, resourceDir, testDir,pomFile);
    }
}
