package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;

public class LiteLoaderTemplate extends AbstractTemplate {

    public static void applyBuildGradleTemplate(@NotNull Project project,
                                                @NotNull VirtualFile file,
                                                @NotNull String groupId,
                                                @NotNull String artifactId,
                                                @NotNull String pluginVersion,
                                                @NotNull String mcVersion,
                                                @NotNull String mcpMappings) {

        Properties properties = new Properties();
        properties.setProperty("GROUP_ID", groupId);
        properties.setProperty("ARTIFACT_ID", artifactId);
        properties.setProperty("VERSION", pluginVersion);
        properties.setProperty("MC_VERSION", mcVersion);
        properties.setProperty("MCP_MAPPINGS", mcpMappings);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_BUILD_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
