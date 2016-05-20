package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

public class ForgeTemplate extends AbstractTemplate {

    public static void applyBuildGradleTemplate(@NotNull Module module,
                                                @NotNull VirtualFile file,
                                                @NotNull String groupId,
                                                @NotNull String artifactId,
                                                @NotNull String pluginVersion,
                                                @Nullable String description,
                                                @NotNull String buildVersion,
                                                @NotNull String repoName,
                                                @NotNull String repoUrl,
                                                @NotNull String depGroupId,
                                                @NotNull String depArtifactId,
                                                @NotNull String depVersion) {

        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", buildVersion);
        properties.setProperty("PLUGIN_VERSION", pluginVersion);
        properties.setProperty("GROUP_ID", groupId);
        if (description != null) {
            properties.setProperty("HAS_DESCRIPTION", "true");
            properties.setProperty("DESCRIPTION", description);
        }
        properties.setProperty("REPO_NAME", repoName);
        properties.setProperty("REPO_URL", repoUrl);
        properties.setProperty("DEP_GROUP_ID", depGroupId);
        properties.setProperty("DEP_ARTIFACT_ID", depArtifactId);
        properties.setProperty("DEP_VERSION", depVersion);

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.BUILD_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
