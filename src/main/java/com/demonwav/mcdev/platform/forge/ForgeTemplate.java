package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;

public class ForgeTemplate extends AbstractTemplate {

    public static void applyBuildGradleTemplate(@NotNull Module module,
                                                @NotNull VirtualFile file,
                                                @NotNull String groupId,
                                                @NotNull String artifactId,
                                                @NotNull String forgeVersion,
                                                @NotNull String mcpVersion,
                                                @NotNull String pluginVersion) {

        Properties properties = new Properties();
        properties.setProperty("GROUP_ID", groupId);
        properties.setProperty("ARTIFACT_ID", artifactId);
        properties.setProperty("PLUGIN_VERSION", pluginVersion);
        properties.setProperty("FORGE_VERSION", forgeVersion);
        properties.setProperty("MCP_VERSION", mcpVersion);

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.FORGE_BUILD_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
