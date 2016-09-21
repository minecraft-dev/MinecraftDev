package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

public abstract class AbstractTemplate {

    @Nullable
    public static String applyBuildGradleTemplate(@NotNull Project project,
                                                  @NotNull VirtualFile file,
                                                  @NotNull String groupId,
                                                  @NotNull String pluginVersion,
                                                  @NotNull String buildVersion) {

        Properties buildGradleProps = new Properties();
        buildGradleProps.setProperty("BUILD_VERSION", buildVersion);

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUILD_GRADLE_TEMPLATE);

        Properties gradleProps = new Properties();
        gradleProps .setProperty("PLUGIN_VERSION", pluginVersion);
        gradleProps .setProperty("GROUP_ID", groupId);

        // create gradle.properties
        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.GRADLE_PROPERTIES_TEMPLATE, gradleProps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return template.getText(buildGradleProps);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void applyMultiModuleBuildGradleTemplate(@NotNull Project project,
                                                           @NotNull VirtualFile file,
                                                           @NotNull VirtualFile prop,
                                                           @NotNull String groupId,
                                                           @NotNull String pluginVersion,
                                                           @NotNull String buildVersion) {

        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", buildVersion);

        Properties gradleProps = new Properties();
        gradleProps .setProperty("PLUGIN_VERSION", pluginVersion);
        gradleProps .setProperty("GROUP_ID", groupId);

        // create gradle.properties
        try {
            applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.GRADLE_PROPERTIES_TEMPLATE, gradleProps);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.MULTI_MODULE_BUILD_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void applySettingsGradleTemplate(@NotNull Project project,
                                                   @NotNull VirtualFile  file,
                                                   @NotNull String projectName,
                                                   @NotNull String includes) {

        Properties properties = new Properties();
        properties.setProperty("PROJECT_NAME", projectName);
        properties.setProperty("INCLUDES", includes);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.SETTINGS_GRADLE_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String applySubmoduleBuildGradleTemplate(@NotNull Project project,
                                                           @NotNull String commonProjectName) {

        Properties properties = new Properties();
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName);

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SUBMODULE_BUILD_GRADLE_TEMPLATE);

        try {
            return template.getText(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static void applyTemplate(@NotNull Project project,
                                        @NotNull VirtualFile file,
                                        @NotNull String templateName,
                                        @NotNull Properties properties) throws IOException {

        applyTemplate(project, file, templateName, properties, false);
    }

    protected static void applyTemplate(@NotNull Project project,
                                        @NotNull VirtualFile file,
                                        @NotNull String templateName,
                                        @NotNull Properties properties,
                                        boolean trimNewlines) throws IOException {

        FileTemplateManager manager = FileTemplateManager.getInstance(project);
        FileTemplate template = manager.getJ2eeTemplate(templateName);

        Properties allProperties = manager.getDefaultProperties();
        allProperties.putAll(properties);

        String text = template.getText(allProperties);
        if (trimNewlines) {
            text = text.replaceAll("\\n+", "\n");
        }
        VfsUtil.saveText(file, text);

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            new ReformatCodeProcessor(project, psiFile, null, false).run();
        }
    }
}
