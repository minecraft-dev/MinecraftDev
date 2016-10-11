/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

public class SpongeTemplate extends AbstractTemplate {

    @Nullable
    public static String applyPomTemplate(@NotNull Project project,
                                          @NotNull String version) {

        final Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        final FileTemplateManager manager = FileTemplateManager.getInstance(project);
        final FileTemplate fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void applyMainClassTemplate(@NotNull Project project,
                                              @NotNull VirtualFile mainClassFile,
                                              @NotNull String packageName,
                                              @NotNull String className,
                                              boolean hasDependencies,
                                              boolean generateDocumentation) {

        final Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);
        if (hasDependencies) {
            properties.setProperty("HAS_DEPENDENCIES", "true");
        }

        if (generateDocumentation) {
            properties.setProperty("GENERATE_DOCUMENTATION", "true");
        }

        try {
            applyTemplate(project, mainClassFile, MinecraftFileTemplateGroupFactory.SPONGE_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String applyBuildGradleTemplate(@NotNull Project project,
                                                  @NotNull VirtualFile file,
                                                  @NotNull String groupId,
                                                  @NotNull String artifactId,
                                                  @NotNull String pluginVersion,
                                                  @NotNull String buildVersion) {

        final Properties properties = new Properties();
        // Only set build version if it is higher/lower than 1.8 (SpongeGradle automatically sets it to 1.8)
        if (!buildVersion.equals("1.8")) {
            properties.setProperty("BUILD_VERSION", buildVersion);
        }

        final FileTemplateManager manager = FileTemplateManager.getInstance(project);
        final FileTemplate template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_BUILD_GRADLE_TEMPLATE);

        final Properties gradleProps = new Properties();
        gradleProps.setProperty("GROUP_ID", groupId);
        gradleProps.setProperty("PLUGIN_ID", artifactId.toLowerCase(Locale.ENGLISH));
        gradleProps.setProperty("PLUGIN_VERSION", pluginVersion);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.SPONGE_GRADLE_PROPERTIES_TEMPLATE, gradleProps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return template.getText(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String applySubmoduleBuildGradleTemplate(@NotNull Project project,
                                                           @NotNull String commonProjectName) {

        final Properties properties = new Properties();
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName);

        final FileTemplateManager manager = FileTemplateManager.getInstance(project);
        final FileTemplate template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE);

        try {
            return template.getText(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
