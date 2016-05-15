package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.buildsystem.BuildDependency;
import com.demonwav.mcdev.buildsystem.BuildRepository;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.google.common.base.Objects;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collections;

public class MinecraftProjectCreator {

    private VirtualFile root = null;
    private String groupId = null;
    private String artifactId = null;
    private String version = null;
    private PlatformType type = PlatformType.BUKKIT;
    private Module module = null;
    private BuildSystem buildSystem;

    private ProjectConfiguration settings = null;

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
        buildSystem.setPluginName(settings.pluginName);

        BuildRepository buildRepository = new BuildRepository();
        BuildDependency dependency = new BuildDependency();
        buildSystem.setRepositories(Collections.singletonList(buildRepository));
        buildSystem.setDependencies(Collections.singletonList(dependency));

        switch (type) {
            case BUKKIT:
                buildRepository.setId("spigotmc-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/groups/public/");
                dependency.setGroupId("org.bukkit");
                dependency.setArtifactId("bukkit");
                dependency.setVersion("1.9.4-R0.1-SNAPSHOT");
                break;
            case SPIGOT:
                buildRepository.setId("spigotmc-repo");
                buildRepository.setUrl("https://hub.spigotmc.org/nexus/content/groups/public/");
                dependency.setGroupId("org.spigotmc");
                dependency.setArtifactId("spigot-api");
                dependency.setVersion("1.9.4-R0.1-SNAPSHOT");
                break;
            case PAPER:
                buildRepository.setId("destroystokyo-repo");
                buildRepository.setUrl("https://repo.destroystokyo.com/content/groups/public/");
                dependency.setGroupId("com.destroystokyo.paper");
                dependency.setArtifactId("paper-api");
                dependency.setVersion("1.9.4-R0.1-SNAPSHOT");
                break;
            case BUNGEECORD:
                buildRepository.setId("sonatype-oss-repo");
                buildRepository.setUrl("https://oss.sonatype.org/content/groups/public/");
                dependency.setGroupId("net.md-5");
                dependency.setArtifactId("bungeecord-api");
                dependency.setVersion("1.9-SNAPSHOT");
                break;
            case SPONGE:
                buildRepository.setId("spongepowered-repo");
                buildRepository.setUrl("https://repo.spongepowered.org/maven/");
                dependency.setGroupId("org.spongepowered");
                dependency.setArtifactId("spongeapi");
                dependency.setVersion("4.0.3");
            default:
                break;
        }
        dependency.setScope("provided");

        buildSystem.create(module, type, settings);
        settings.create(module, type, buildSystem);
        buildSystem.finishSetup(module, type, settings);
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

    public PlatformType getType() {
        return type;
    }

    public void setType(PlatformType type) {
        this.type = type;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public ProjectConfiguration getSettings() {
        return settings;
    }

    public void setSettings(ProjectConfiguration settings) {
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
        if (type != that.type) {
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
                type,
                module,
                settings,
                sourceDir,
                resourceDir,
                testDir,
                pomFile
        );
    }
}
