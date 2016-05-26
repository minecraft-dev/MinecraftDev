package com.demonwav.mcdev.platform;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

public abstract class AbstractModuleType {

    @NotNull
    private final String groupId;
    @NotNull
    private final String artifactId;

    public AbstractModuleType(@NotNull final String groupId, @NotNull final String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    @NotNull
    public String getGroupId() {
        return groupId;
    }

    @NotNull
    public String getArtifactId() {
        return artifactId;
    }

    public abstract PlatformType getPlatformType();

    public abstract Icon getBigIcon();

    public abstract Icon getIcon();

    public abstract String getId();

    @NotNull
    public abstract List<String> getIgnoredAnnotations();

    @NotNull
    public abstract List<String> getListenerAnnotations();
}
