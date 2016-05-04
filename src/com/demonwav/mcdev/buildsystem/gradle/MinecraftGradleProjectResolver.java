package com.demonwav.mcdev.buildsystem.gradle;

import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

public abstract class MinecraftGradleProjectResolver extends AbstractProjectResolverExtension {

    private final String GROUP_ID;
    private final String ARTIFACT_ID;

    public MinecraftGradleProjectResolver(String GROUP_ID, String ARTIFACT_ID) {
        this.GROUP_ID = GROUP_ID;
        this.ARTIFACT_ID = ARTIFACT_ID;
    }

    // TODO: impl
}
