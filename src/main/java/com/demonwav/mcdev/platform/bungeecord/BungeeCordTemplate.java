/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem;
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem;
import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Properties;

public class BungeeCordTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(@NotNull Project project,
                                              @NotNull VirtualFile file,
                                              @NotNull String packageName,
                                              @NotNull String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUNGEECORD_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static String applyPomTemplate(@NotNull Project project,
                                          @NotNull String version) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUNGEECORD_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            // TODO what to do when this fails?
            e.printStackTrace();
            return "";
        }
    }

    public static void applyPluginDescriptionFileTemplate(@NotNull Project project,
                                                          @NotNull VirtualFile file,
                                                          @NotNull BungeeCordProjectConfiguration settings,
                                                          @NotNull BuildSystem buildSystem) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);

        if (buildSystem instanceof GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@");
        } else if (buildSystem instanceof MavenBuildSystem) {
            properties.setProperty("VERSION", "${project.version}");
        }

        properties.setProperty("MAIN", settings.mainClass);

        if (settings.hasDependencies()) {
            properties.setProperty("DEPEND", settings.dependencies.toString());
            properties.setProperty("HAS_DEPEND", "true");
        }

        if (settings.hasSoftDependencies()) {
            properties.setProperty("SOFT_DEPEND", settings.softDependencies.toString());
            properties.setProperty("HAS_SOFT_DEPEND", "true");
        }

        if (settings.hasAuthors()) {
            // BungeeCord only supports one author
            properties.setProperty("AUTHOR", settings.authors.get(0));
            properties.setProperty("HAS_AUTHOR", "true");
        }

        if (settings.hasDescription()) {
            properties.setProperty("DESCRIPTION", settings.description);
            properties.setProperty("HAS_DESCRIPTION", "true");
        }

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
