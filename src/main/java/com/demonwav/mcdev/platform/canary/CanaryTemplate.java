package com.demonwav.mcdev.platform.canary;

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

public class CanaryTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(@NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull String packageName,
            @NotNull String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.CANARY_MAIN_CLASS_TEMPLATE, properties);
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
        FileTemplate fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.CANARY_POM_TEMPLATE);
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
            @NotNull CanaryProjectConfiguration settings,
            @NotNull BuildSystem buildSystem) {
        Properties properties = new Properties();

        properties.setProperty("NAME", settings.pluginName);

        if (buildSystem instanceof GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@");
        } else if (buildSystem instanceof MavenBuildSystem) {
            properties.setProperty("VERSION", "${project.version}");
        }

        properties.setProperty("MAIN", settings.mainClass);

        if (settings.hasAuthors()) {
            properties.setProperty("AUTHOR_LIST", settings.authors.toString());
            properties.setProperty("HAS_AUTHOR_LIST", "true");
        }

        // TODO: Does Canary support description?

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.CANARY_INF_TEMPLATE, properties, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
