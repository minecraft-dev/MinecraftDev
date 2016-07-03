package com.demonwav.mcdev.creator;

import static com.demonwav.mcdev.platform.PlatformType.FORGE;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.demonwav.mcdev.platform.bukkit.BukkitProjectConfiguration;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordProjectConfiguration;
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration;
import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinecraftProjectCreator {

    public int index = 0;

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private Module module = null;
    private BuildSystem buildSystem;

    private List<ProjectConfiguration> settings = new ArrayList<>();

    private VirtualFile sourceDir;
    private VirtualFile resourceDir;
    private VirtualFile testDir;
    private VirtualFile pomFile;

    public void create() {
        buildSystem.setRootDirectory(root);

        buildSystem.setGroupId(groupId);
        buildSystem.setArtifactId(artifactId);
        buildSystem.setVersion(version);

        //buildSystem.setPluginAuthor(settings.author); // TODO: build systems have "developer" blocks
        buildSystem.setPluginName(settings.get(0).pluginName);

        List<BuildRepository> buildRepositories = new ArrayList<>();
        List<BuildDependency> dependencies = new ArrayList<>();
        buildSystem.setRepositories(buildRepositories);
        buildSystem.setDependencies(dependencies);

        if (settings.size() == 1) {
            doSingleModuleCreate();
        } else {
            doMultiModuleCreate();
        }
    }

    private void doSingleModuleCreate() {
        ProjectConfiguration configuration = settings.get(0);
        addDependencies(configuration, buildSystem.getRepositories(), buildSystem.getDependencies());

        ProgressManager.getInstance().run(new Task.Backgroundable(module.getProject(), "Setting Up Project", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                buildSystem.create(module.getProject(), configuration, indicator);
                configuration.create(module.getProject(), buildSystem, indicator);
                configuration.type.getType().performCreationSettingSetup(module.getProject());
                buildSystem.finishSetup(module, configuration, indicator);
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
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                Map<GradleBuildSystem, ProjectConfiguration> map = gradleBuildSystem.createMultiModuleProject(module.getProject(), settings, indicator);
                map.forEach((g, p) -> {
                    p.create(module.getProject(), g, indicator);
                    p.type.getType().performCreationSettingSetup(module.getProject());
                });
                gradleBuildSystem.finishSetup(module, null, indicator);
            }
        });
    }

    public static void addDependencies(ProjectConfiguration configuration, List<BuildRepository> buildRepositories, List<BuildDependency> buildDependencies) {
        // Forge doesn't have a dependency like this
        if (configuration.type == FORGE) {
            return;
        }

        BuildRepository buildRepository = new BuildRepository();
        BuildDependency buildDependency = new BuildDependency();

        buildRepositories.add(buildRepository);
        buildDependencies.add(buildDependency);
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
                addSonatype(buildRepositories);
                break;
            case PAPER:
                buildRepository.setId("destroystokyo-repo");
                buildRepository.setUrl("https://repo.destroystokyo.com/content/groups/public/");
                buildDependency.setGroupId("com.destroystokyo.paper");
                buildDependency.setArtifactId("paper-api");
                buildDependency.setVersion(((BukkitProjectConfiguration) configuration).minecraftVersion + "-R0.1-SNAPSHOT");
                addSonatype(buildRepositories);
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
                buildRepository.setUrl("https://repo.spongepowered.org/maven/");
                buildDependency.setGroupId("org.spongepowered");
                buildDependency.setArtifactId("spongeapi");
                if (configuration instanceof SpongeProjectConfiguration) {
                    buildDependency.setVersion(((SpongeProjectConfiguration) configuration).spongeApiVersion + "-SNAPSHOT");
                } else {
                    buildDependency.setVersion(((SpongeForgeProjectConfiguration) configuration).spongeApiVersion + "-SNAPSHOT");
                }
            default:
                break;
        }
        buildDependency.setScope("provided");
    }

    private static void addSonatype(List<BuildRepository> buildRepositories) {
        buildRepositories.add(new BuildRepository("sonatype", "https://oss.sonatype.org/content/groups/public/"));
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

    public List<ProjectConfiguration> getSettings() {
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
        return "MinecraftProjectCreator{" +
                "root=" + root +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", module=" + module +
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

        if (!Objects.equal(this.root, that.root)) {
            return false;
        }
        if (!Objects.equal(this.groupId, that.groupId)) {
            return false;
        }
        if (!Objects.equal(this.artifactId, that.artifactId)) {
            return false;
        }
        if (!Objects.equal(this.version, that.version)) {
            return false;
        }
        if (!Objects.equal(this.module, that.module)) {
            return false;
        }
        if (!Objects.equal(this.settings, that.settings)) {
            return false;
        }
        if (!Objects.equal(this.sourceDir, that.sourceDir)) {
            return false;
        }
        if (!Objects.equal(this.resourceDir, that.resourceDir)) {
            return false;
        }
        if (!Objects.equal(this.testDir, that.testDir)) {
            return false;
        }
        return Objects.equal(this.pomFile, that.pomFile);

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                root,
                groupId,
                artifactId,
                version,
                module,
                settings,
                sourceDir,
                resourceDir,
                testDir,
                pomFile
        );
    }
}
