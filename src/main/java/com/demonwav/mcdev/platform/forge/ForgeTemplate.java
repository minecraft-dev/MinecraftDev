package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static void applyMcmodInfoTemplate(@NotNull Module module,
                                              @NotNull VirtualFile file,
                                              @NotNull String artifactId,
                                              @NotNull String pluginName,
                                              @NotNull String description,
                                              @NotNull String url,
                                              @NotNull String updateUrl,
                                              @Nullable String authorList,
                                              @Nullable String dependenciesList) {

        Properties properties = new Properties();
        properties.setProperty("ARTIFACT_ID", artifactId);
        properties.setProperty("PLUGIN_NAME", pluginName);
        properties.setProperty("DESCRIPTION", description);
        properties.setProperty("URL", url);
        properties.setProperty("UPDATE_URL", updateUrl);

        if (authorList != null) {
            properties.setProperty("HAS_AUTHOR_LIST", "true");
            properties.setProperty("AUTHOR_LIST", authorList);
        }

        if (dependenciesList != null) {
            properties.setProperty("HAS_DEPENDENCIES_LIST", "true");
            properties.setProperty("DEPENDENCIES_LIST", dependenciesList);
        }

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.MCMOD_INFO_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applyMainClassTemplate(@NotNull Module module,
                                              @NotNull VirtualFile file,
                                              @NotNull String packageName,
                                              @NotNull String artifactId,
                                              @NotNull String pluginName,
                                              @NotNull String pluginVersion,
                                              @NotNull String className) {

        Properties properties = new Properties();
        properties.setProperty("PACKAGE_NAME", packageName);
        properties.setProperty("ARTIFACT_ID", artifactId);
        properties.setProperty("PLUGIN_NAME", pluginName);
        properties.setProperty("PLUGIN_VERSION", pluginVersion);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(module, file, MinecraftFileTemplateGroupFactory.FORGE_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
